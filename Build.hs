#!/usr/bin/env stack
{- stack
    runghc
    --package filemanip
    --package HStringTemplate
    --package regex-compat
    --package shake
    --package split
-}

import Control.Monad
import Data.Monoid
import Data.List
import Data.List.Split
import Development.Shake
import Development.Shake.Command
import Development.Shake.FilePath
import Development.Shake.Util
import qualified System.Directory as Dir
import System.Environment (getEnvironment)
import System.Exit
import System.Info
import System.Process
import System.Process.Internals
import Text.Regex
import Text.StringTemplate

clojureVersion = "1.9.0-alpha17"
appVersion = "0.0.1"

heapSize = "1g"
appDir = "app"
projectClj = appDir </> "project.clj"
appDeployedJar = "lib" </> "app.jar"
appUberJar = appDir </> "target" </> "app.jar"
versionTxtApp = appDir </> "resources" </> "version.txt"

shakeOpts = shakeOptions { shakeFiles="_build"
                         , shakeTimings=True}

-- http://stackoverflow.com/a/27388709/3227
getPid :: ProcessHandle -> IO (Maybe PHANDLE)
getPid ph = withProcessHandle ph go
  where
    go ph_ = case ph_ of
               OpenHandle x   -> return $ Just x
               ClosedHandle _ -> return Nothing

cleanUpProcess :: ProcessHandle -> String -> IO ()
cleanUpProcess hdl desc = do
  putStrLn $ ">>>>> terminating " <> desc
  terminateProcess $ hdl


withProcess :: String -> CreateProcess -> Action () -> Action ()
withProcess name p action = do
  (_, _, _, handle) <- liftIO . createProcess $ p
  (Just pid) <- liftIO . getPid $ handle
  let desc = name <> " (" <> show pid <> ")"
  putNormal $ ">>>>> starting " <> desc
  flip actionFinally (cleanUpProcess handle desc) action


dirtyGit :: IO Bool
dirtyGit = do
  (code, _, _) <- readProcessWithExitCode
    "git" ["diff-index"
          , "--quiet"
          , "HEAD"
          ] ""
  pure $ case code of
    ExitSuccess -> False
    (ExitFailure _) -> True


gitVersion :: IO String
gitVersion = do
  dirt <- dirtyGit
  date <- readCreateProcess
             (proc "git"
                   [ "log"
                   , "--pretty=format:%cI"
                   , "-1"
                   ]) ""
  sha <- readCreateProcess
             (proc "git"
                   [ "log"
                   , "--pretty=format:%h"
                   , "-1"
                   ]) ""
  pure $ (dropLast4 . justNums $ date) <> "-" <> sha <> if dirt then "-dev" else ""
  where
    justNums s = subRegex (mkRegex "[^0-9]+") s ""
    dropLast4 s = reverse . (drop 4) . reverse $ s


main :: IO ()
main = shakeArgs shakeOpts $ do
  want [ "info"
       , projectClj
       , appDeployedJar
       ]

  phony "clean" $ do
    putNormal "cleaning..."
    projectCljExists <- doesFileExist $ appDir </> "project.clj"
    when projectCljExists $ do
      unit $ cmd (Cwd appDir) "lein clean"
    removeFilesAfter "_build" ["//*"]
    removeFilesAfter "lib" ["//*.jar"]
    removeFilesAfter appDir ["project.clj"]
    removeFilesAfter appDir ["resources/version.txt"]

  phony "info" $ do
    ver <- liftIO gitVersion
    unit $ cmd "which lein"
    unit $ cmd "which stack"
    putNormal $ "os: " <> os
    putNormal $ "arch: " <> arch
    putNormal $ "gitVersion: " <> ver

  phony "test-jetty" $ do
    env' <- liftIO getEnvironment
    need [ appDeployedJar]
    let
      -- This wasn't ideal because the compilations from the `lein
      -- run` and the `lein test` would actually happen concurrently.
      -- However, I forgot the trampoline before (to properly replace
      -- the parent process), and want to remember it in case I move
      -- back.
      --   p = (proc "lein" [ "trampoline"
      --                    , "run"
      --                    , "-m"
      --                    , "triweb.boot" ])
      --       { cwd = Just appDir }
      p = (proc "java" [ "-cp"
                       , appDeployedJar
                       , "triweb.boot"
                       ])
          { env = Just ([ ("DEV", "true")
                        , ("PROFILE", "aws-test")] <> env') }
    withProcess "jetty" p $ do
      unit $ cmd [ Cwd appDir
                 , AddEnv "ES" "http://localhost:9200"
                 ]
        -- "lein do clean, test :only tileprox.proxy-test/ping, test :only tileprox.proxy-test/license"
        "lein do clean, test"

  phony "run-jetty" $ do
    need [ appDeployedJar]
    unit $ cmd [ AddEnv "DEV" "true"
               ]
      "java" "-cp" appDeployedJar
      ("-Xmx" <> heapSize)
      ("-Xms" <> heapSize)
      "triweb.boot"

  phony "docker-build" $ do
    ver <- liftIO gitVersion
    need [ appDeployedJar
         ]
    cmd
      [ "docker"
      , "build"
      , "-t"
      , "container-registry-test.elastic.co/infra/tileprox:" <> ver
      , "."
      ]

  phony "docker-run" $ do
    ver <- liftIO gitVersion
    need [ "docker-build"
         ]
    cmd
      [ "docker"
      , "run"
      , "-t"
      , "-i"
      , "-p"
      , "9000:9000"
      , "container-registry-test.elastic.co/infra/tileprox:" <> ver
      ]

  phony "docker-push-staging" $ do
    ver <- liftIO gitVersion
    need [ "docker-build"
         ]
    unit $ cmd
      [ "docker"
      , "push"
      , "container-registry-test.elastic.co/infra/tileprox:" <> ver
      ]

  phony "docker-push-production" $ do
    ver <- liftIO gitVersion
    need [ "docker-build"
         ]
    unit $ cmd
      [ "docker"
      , "tag"
      , "container-registry-test.elastic.co/infra/tileprox:" <> ver
      , "push.docker.elastic.co/infra/tileprox:" <> ver
      ]
    cmd
      [ "docker"
      , "push"
      , "push.docker.elastic.co/infra/tileprox:" <> ver
      ]

  projectClj %> \out -> do
    let cljTmpl = projectClj <> ".st"
    need [ cljTmpl ]
    confText <- liftIO . readFile $ cljTmpl
    cwd <- liftIO Dir.getCurrentDirectory
    let tmpl = newSTMP confText :: StringTemplate String
        rendered = render $
          setAttribute "appVersion" appVersion $
          setAttribute "clojureVersion" clojureVersion tmpl
    liftIO $ writeFile out rendered

  versionTxtApp %> \out -> do
    ver <- liftIO gitVersion
    liftIO $ writeFile out ver

  appUberJar %> \out -> do
    need [ projectClj
         , versionTxtApp
         ]
    unit $ cmd [ Cwd appDir
               , AddEnv "LEIN_SNAPSHOTS_IN_RELEASE" "true" ]
            "lein with-profile server uberjar"

  appDeployedJar %> \out -> do
    need [ appUberJar
         ]
    copyFile' appUberJar out


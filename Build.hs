#!/usr/bin/env stack
{- stack
    --nix
    --no-nix-pure
    --resolver=lts-11.17
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

clojureVersion = "1.9.0-beta1"
appVersion = "0.0.1"

maintainer = "Drew Raines <web@trinitynashville.org>"
heapSize = "1g"
gcrRoot = "gcr.io"
gcrProject = "trinitynashville-188115"
gcrTagName = "media"
gcrInstanceName = "media-service"
appDir = "app"
projectClj = appDir </> "project.clj"
appUberWar = appDir </> "target" </> "app.war"
appUberJar = appDir </> "target" </> "app.jar"
versionTxtApp = appDir </> "resources" </> "version.txt"
dockerDir = "docker"
dockerFileTmpl = "Dockerfile.app.st"
dockerFile = dockerDir </> "Dockerfile"
appDockerWar = dockerDir </> "app.war"
appDockerJar = dockerDir </> "app.jar"
sermonJson = dockerDir </> "sermons.json.gz"

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

makeGcrImageName :: String -> String
makeGcrImageName ver = gcrRoot <> "/" <> gcrProject <> "/" <> gcrTagName <> ":" <> ver

main :: IO ()
main = shakeArgs shakeOpts $ do
  want [ "info"
       , projectClj
       , appDockerWar
       ]

  phony "clean" $ do
    putNormal "cleaning..."
    projectCljExists <- doesFileExist $ appDir </> "project.clj"
    when projectCljExists $ do
      unit $ cmd (Cwd appDir) "rm -rf target"
    removeFilesAfter "_build" ["//*"]
    removeFilesAfter "docker" ["//*"]
    removeFilesAfter "lib" ["//*.jar", "//*.war"]
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
    need [ appUberJar ]
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
                       , appUberJar
                       , "triweb.boot"
                       ])
          { env = Just ([ ("DEV", "true") ] <> env') }
    withProcess "jetty" p $ do
      unit $ cmd [ Cwd appDir
                 , AddEnv "ES" "http://localhost:9200"
                 ]
        "lein do clean, test"

  phony "run-jetty" $ do
    need [ projectClj ]
    unit $ cmd [ AddEnv "DEV" "true"
               , Cwd appDir
               ]
      "lein" "ring" "server-headless"

  phony "docker-build" $ do
    ver <- liftIO gitVersion
    need [ appDockerJar
         , dockerFile
         , sermonJson
         ]
    cmd
      [ Cwd dockerDir ]
      [ "docker"
      , "build"
      , "-t"
      , "trinitynashville/media:" <> ver
      , "."
      ]

  phony "docker-tag-gcr" $ do
    ver <- liftIO gitVersion
    need [ "docker-build"
         ]
    cmd
      [ "docker"
      , "tag"
      , "trinitynashville/media:" <> ver
      , makeGcrImageName ver
      ]
    
  phony "docker-push-gcr" $ do
    ver <- liftIO gitVersion
    need [ "docker-tag-gcr"
         ]
    cmd
      [ "docker"
      , "push"
      , makeGcrImageName ver
      ]

  phony "gcr-update" $ do
    ver <- liftIO gitVersion
    need [ "docker-push-gcr"
         ]
    cmd
      [ "kubectl"
      , "set"
      , "image"
      , "deployment/" <> gcrProject
      , gcrProject <> "=" <> makeGcrImageName ver
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
      , "trinitynashville/media:" <> ver
      ]

  dockerFile %> \out -> do
    need [ dockerFileTmpl ]
    confText <- liftIO . readFile $ dockerFileTmpl
    let tmpl = newSTMP confText :: StringTemplate String
        rendered = render $
          setAttribute "maintainer" maintainer tmpl
    liftIO $ writeFile out rendered

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

  appUberWar %> \out -> do
    need [ projectClj
         , versionTxtApp
         ]
    unit $ cmd [ Cwd appDir
               , AddEnv "LEIN_SNAPSHOTS_IN_RELEASE" "true" ]
            "lein ring uberwar"

  appUberJar %> \out -> do
    need [ projectClj
         , versionTxtApp
         ]
    unit $ cmd [ Cwd appDir
               , AddEnv "LEIN_SNAPSHOTS_IN_RELEASE" "true" ]
            "lein with-profile package uberjar"

  sermonJson %> \out -> do
    let sermonStaging = (appDir </> "target" </> "sermons.json.gz")
    need [ projectClj
         , versionTxtApp
         ]
    unit $ cmd [ Cwd appDir
               , AddEnv "LEIN_SNAPSHOTS_IN_RELEASE" "true" ]
      "lein with-profile sermon-compile run -m triweb.media/compile-sermons -- search/"
    copyFile' sermonStaging out

  appDockerWar %> \out -> do
    let archive = appUberWar
    need [ archive
         ]
    copyFile' archive out

  appDockerJar %> \out -> do
    let archive = appUberJar
    need [ archive
         ]
    copyFile' archive out

#!/usr/bin/env stack
{- stack
     --resolver lts-6.10
     runghc
     --package turtle
     --package yaml
     --package system-filepath
     --package foldl
 -}

{-# LANGUAGE OverloadedStrings #-}
{-# LANGUAGE TemplateHaskell #-}

import Turtle hiding (option)
import GHC.IO.Exception
import Data.Yaml (decodeEither)
import Data.Aeson.TH ( deriveJSON
                     , defaultOptions
                     , fieldLabelModifier
                     , constructorTagModifier
                     )
import Data.Text (Text, unpack, pack, intercalate)
import Data.Char (toLower)
import qualified Data.ByteString.Char8 as B
import Options.Applicative as Options
import Options.Applicative ( execParser
                           , info
                           , helper
                           , header
                           , argument
                           , str
                           , metavar
                           , option
                           , auto
                           , strOption
                           , long
                           , value
                           , optional )
import System.FilePath.Posix (takeBaseName, takeFileName)
import System.Directory
import Filesystem.Path.CurrentOS (fromText)
import qualified Control.Foldl as Fold

defaultBucket = "media.trinitynashville.org"
defaultAuthor = "Trinity Church"
defaultImage  = "resources/static/img/podcast4.png"
defaultScale  = 3

type Preset = Text
type Suffix = Text

class Dated a where
  datedString :: a -> Text

class Encodable a where
  encodeTitle :: Dated b => a -> b -> Text

instance Dated Opts where
  datedString (Opts {setDate = (Just date)}) = pack date
  datedString o@(Opts {setDate = Nothing}) =
    pack . takeBaseName . mp3SrcPath $ o

instance Encodable PodcastInput where
  encodeTitle pi o = (datedString o) <> "-" <> (pcFilePrefix pi)

data Opts = Opts { mp3SrcPath  :: String
                 , bucket      :: String
                 , imagePath   :: String
                 , audioScale  :: Int
                 , albumAuthor :: String
                 , setDate :: Maybe String
                 } deriving (Show)

opts :: Options.Parser Opts
opts = Opts <$> argument str (metavar "MP3")
       <*> strOption
           ( long "bucket"
           <> metavar "BUCKET"
           <> help "What bucket?"
           <> value defaultBucket )
       <*> strOption
           ( long "imagePath"
           <> metavar "IMAGE"
           <> help "What image to attach to the podcast"
           <> value defaultImage )
       <*> option auto
           ( long "audioScale"
           <> metavar "IMAGE"
           <> help "What image to attach to the podcast"
           <> value defaultScale )
       <*> strOption
           ( long "albumAuthor"
           <> metavar "ALBUM AUTHOR"
           <> help "What image to attach to the podcast"
           <> value defaultAuthor )
       <*> ( optional $ strOption
             ( long "setDate"
             <> metavar "YYYY-MM-DD"
             <> help "Date prefix override" ) )

data PodcastInput = PodcastInput
  { pcFilePrefix :: Text
  , pcAuthor     :: Text
  , pcYear       :: Int
  , pcComposer   :: Text
  , pcAlbum      :: Text
  , pcGenre      :: Text
  , pcComment    :: Text
  } deriving (Show)

$(deriveJSON
  defaultOptions { fieldLabelModifier = (map toLower) . (drop 2)
                 } ''PodcastInput)

data Context = Context { ctxOpts :: Opts
                       , ctxPi :: PodcastInput
                       } deriving (Show)

makeContext :: Opts -> PodcastInput -> Context
makeContext o pi = Context o pi

encode :: Context -> Preset -> Suffix -> IO ()
encode ctx preset suffix = do
  let pi = ctxPi ctx
      o = ctxOpts ctx
      title = (encodeTitle pi o) <> suffix
      dest = title <> ".mp3"
  exist <- testfile . fromText $ dest
  when (not exist) $ do
    let scale = audioScale . ctxOpts $ ctx
        src = pack . mp3SrcPath . ctxOpts $ ctx
    lame preset scale src dest
    setVersion dest
    addMeta ctx title dest
    rt <- runtime dest
    upload ctx dest rt
    echo (format ("*** LINK: http://"%s%"/"%s) (pack . bucket $ o) dest)

lame :: Text -> Int -> Text -> Text -> IO ()
lame preset scale src dest =
  loggingProc "lame"
       [ "-S"
       , "--scale", (pack . show $ scale)
       , "--preset", preset
       , src
       , dest
       ]

setVersion dest = loggingProc "eyeD3" [ "--to-v2.4", dest]

runtime :: Text -> IO (Text)
runtime dest = do
  rt <- fold (inshell (format ("eyeD3 "%s%" | fgrep Time | awk '{print $2}' | perl -pe 's,\\e\\[22m([0-9:]+),$1,'") dest) empty) Fold.head
  case rt of
    (Just rt') -> return rt'
    Nothing -> die (format ("cannot get runtime from "%s) dest)

addMeta :: Context -> Text -> Text -> IO ()
addMeta ctx title dest =
  let o = ctxOpts ctx
      pi = ctxPi ctx
  in
    loggingProc "eyeD3"
         [ "--add-image", (pack (imagePath o ++ ":ICON"))
         , "--release-year", (pack . show . pcYear $ pi)
         , "--artist", pcComposer pi
         , "--album", pcAlbum pi
         , "--album-artist", (pack . albumAuthor $ o)
         , "--title", title
         , "--genre", (pcGenre pi)
         , "--add-comment", (pcComment pi)
         , "--encoding", "utf8"
         , dest
         ]

upload :: Context -> Text -> Text -> IO ()
upload ctx dest dur =
  let o = ctxOpts ctx
      pi = ctxPi ctx
  in
    loggingProc "aws"
         [ "s3api", "put-object"
         , "--acl", "public-read"
         , "--metadata", "runtime=" <> dur
         , "--content-type", "audio/mpeg"
         , "--body", dest
         , "--bucket", (pack . bucket $ o)
         , "--key", (pack . takeFileName . unpack $ dest)
         ]

loggingProc :: Text -> [Text] -> IO ()
loggingProc cmd args = do
  echo $ format ("running: "%s%" "%s) cmd (intercalate " " args)
  ret <- proc cmd args empty
  case ret of
     ExitSuccess -> return ()
     ExitFailure err -> die (cmd <> " has failed: " <> (repr err))

loggingShell :: Text -> IO ()
loggingShell cmd = do
  echo $ format ("running: "%s) cmd
  ret <- shell cmd empty
  case ret of
     ExitSuccess -> return ()
     ExitFailure err -> die (cmd <> " has failed: " <> (repr err))

main :: IO ()
main = do
  o <- execParser $ info (helper <*> opts) (header "Trinity Audio Encoder")
  j <- getContents
  case (decodeEither (B.pack j) :: Either String PodcastInput) of
    (Left err) -> putStrLn $ "\n*** problem reading information file:\n\n" ++ err ++ "\n"
    (Right pi) -> do
      let ctx = makeContext o pi
      encode ctx "voice" ""
      encode ctx "320" "-HIFI"

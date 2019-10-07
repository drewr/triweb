#!/usr/bin/env stack
{- stack
     --resolver lts-14.7
     --install-ghc
     --nix
     --no-nix-pure
     runghc
     --package turtle
     --package yaml
     --package system-filepath
     --package foldl
 -}

{-# LANGUAGE OverloadedStrings #-}
{-# LANGUAGE TemplateHaskell #-}

import Control.Monad
import Turtle hiding (option)
import Turtle.Pattern hiding (option)
import GHC.IO.Exception
import Data.Map (Map(..))
import Data.Aeson (eitherDecode)
import Data.Aeson.TH ( deriveJSON
                     , defaultOptions
                     , fieldLabelModifier
                     , constructorTagModifier
                     )
import Data.Text (Text, unpack, pack, intercalate)
import Data.Char (toLower)
import qualified Data.ByteString.Lazy.Char8 as B
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

defaultBucket = "trinitynashville-media"
defaultAuthor = "Trinity Church"
defaultGenre  = "Speech"
defaultImage  = "resources/static/img/podcast5.png"
defaultScale  = 3

type Preset = Text
type Suffix = Text

class Dated a where
  datedString :: a -> Text
  year :: a -> Int

class Encodable a where
  encodeTitle :: Dated b => a -> b -> Text

instance Dated Opts where
  datedString (Opts {setDate = (Just date)}) = pack date
  datedString o@(Opts {setDate = Nothing}) =
    pack . takeBaseName . mp3SrcPath $ o
  year (Opts {setDate = (Just date)}) = read $ take 4 date :: Int

instance Encodable PodcastSource where
  encodeTitle pi o = (datedString o) <> "-" <> (pcSlug pi)

data Opts = Opts { mp3SrcPath  :: String
                 , bucket      :: String
                 , imagePath   :: String
                 , audioScale  :: Int
                 , albumAuthor :: String
                 , genre       :: String
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
           <> metavar "SCALE"
           <> help "Audio scale"
           <> value defaultScale )
       <*> strOption
           ( long "albumAuthor"
           <> metavar "ALBUM AUTHOR"
           <> help "Album author"
           <> value defaultAuthor )
       <*> strOption
           ( long "genre"
           <> metavar "GENRE"
           <> help "Podcast genre category"
           <> value defaultGenre )
       <*> ( optional $ strOption
             ( long "setDate"
             <> metavar "YYYY-MM-DD"
             <> help "Date prefix override" ) )

data PodcastSource = PodcastSource
  { pcSeries    :: Text
  , pcSpeaker   :: Text
  , pcTitle     :: Text
  , pcScripture :: [Map String Int]
  , pcSlug      :: Text
  , pcBlurb     :: Text
  } deriving (Show)

$(deriveJSON
  defaultOptions { fieldLabelModifier = (map toLower) . (drop 2)
                 } ''PodcastSource)

data Context = Context { ctxOpts :: Opts
                       , ctxPi :: PodcastSource
                       } deriving (Show)

data Runtime = Runtime
  { duration :: Text
  , seconds :: Integer
  }


makeContext :: Opts -> PodcastSource -> Context
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
    printf ("*** LINK: https://storage.googleapis.com/"%s%"/"%s%"\n") (pack . bucket $ o) dest

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

parseSecs :: Text -> Integer
parseSecs dur = secs'
  where
    ((mins:secs:_):_) = match (decimal `sepBy` ":") dur
    secs' = mins * 60 + secs

runtime :: Text -> IO Runtime
runtime dest = do
  rt <- fold (inshell (format ("eyeD3 "%s%" | egrep ^Time | awk '{print $2}'") dest) empty) Fold.head
  case rt of
    (Just rt') -> do
        let rt'' = lineToText rt'
        return $ Runtime { duration=rt'', seconds=parseSecs rt''}
    Nothing -> die (format ("cannot get runtime from "%s) dest)

addMeta :: Context -> Text -> Text -> IO ()
addMeta ctx title dest =
  let o = ctxOpts ctx
      pi = ctxPi ctx
  in
    loggingProc "eyeD3"
         [ "--add-image", (pack (imagePath o ++ ":ICON"))
         , "--release-year", (pack . show . year $ o)
         , "--artist", pcSpeaker pi
         , "--album", pack . albumAuthor $ o
         , "--album-artist", (pack . albumAuthor $ o)
         , "--title", title
         , "--genre", pack . genre $ o
         , "--add-comment", (pcBlurb pi)
         , "--encoding", "utf8"
         , dest
         ]

upload :: Context -> Text -> Runtime -> IO ()
upload ctx dest rt =
  let o = ctxOpts ctx
      pi = ctxPi ctx
  in
    loggingProc "gsutil"
         [ "-h", "Content-Type:audio/mpeg"
         , "-h", "x-goog-meta-duration:" <> duration rt
         , "-h", "x-goog-meta-seconds:" <> ( pack . show . seconds $ rt )
         , "cp"
         , "-a", "public-read"
         , (pack . takeFileName . unpack $ dest)
         , "gs://" <> (pack . bucket $ o) <> "/" 
         ]

loggingProc :: Text -> [Text] -> IO ()
loggingProc cmd args = do
  printf ("running: "%s%" "%s%"\n") cmd (intercalate " " args)
  ret <- proc cmd args empty
  case ret of
     ExitSuccess -> return ()
     ExitFailure err -> die (cmd <> " has failed: " <> (repr err))

loggingShell :: Text -> IO ()
loggingShell cmd = do
  printf ("running: "%s%"\n") cmd
  ret <- shell cmd empty
  case ret of
     ExitSuccess -> return ()
     ExitFailure err -> die (cmd <> " has failed: " <> (repr err))

main :: IO ()
main = do
  o <- execParser $ info (helper <*> opts) (Options.Applicative.header "Trinity Audio Encoder")
  j <- getContents
  case (eitherDecode (B.pack j) :: Either String PodcastSource) of
    (Left err) -> putStrLn $ "\n*** problem reading information file:\n\n" ++ err ++ "\n"
    (Right pi) -> do
      let ctx = makeContext o pi
      encode ctx "voice" ""
      encode ctx "320" "-HIFI"

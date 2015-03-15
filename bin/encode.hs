#!/usr/bin/env runhaskell

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
                           , value)
import System.FilePath.Posix (takeBaseName)
import System.Directory
import Filesystem.Path.CurrentOS (fromText)

defaultBucket = "media.trinitynashville.org"
defaultAuthor = "Trinity Church"
defaultImage  = "../resources/static/img/podcast4.png"
defaultScale  = 3

main :: IO ()
main = do
  o <- execParser $ info (helper <*> opts) (header "Trinity Audio Encoder")
  j <- getContents
  case (decodeEither (B.pack j) :: Either String PodcastInput) of
    (Left err) -> putStrLn $ "\n*** problem reading information file:\n\n" ++ err ++ "\n"
    (Right pi) -> do
      encode "voice" (audioScale o) (pack . mp3SrcPath $ o) (encodeDest pi o)
      encode "320" (audioScale o) (pack . mp3SrcPath $ o) (encodeDestHifi pi o)
      return ()

encode :: Text -> Int -> Text -> Text -> IO ()
encode preset scale src dest = do
  exist <- testfile . fromText $ dest
  when (not exist) $ do
    lame preset scale src dest

lame :: Text -> Int -> Text -> Text -> IO ()
lame preset scale src dest =
  loggingProc "lame"
       [ "-S"
       , "--scale", (pack . show $ scale)
       , "--preset", preset
       , src
       , dest
       ]

loggingProc :: Text -> [Text] -> IO ()
loggingProc cmd args = do
  echo $ format ("running: "%s%" "%s) cmd (intercalate " " args)
  ret <- proc cmd args empty
  case ret of
     ExitSuccess -> return ()
     ExitFailure err -> die (cmd <> " has failed: " <> (repr err))

class Dated a where
  datedString :: a -> Text

class Encodable a where
  encodeTitle    :: Dated b => a -> b -> Text
  encodeDestHifi :: Dated b => a -> b -> Text
  encodeDest     :: Dated b => a -> b -> Text

instance Dated Opts where
  datedString o = pack . takeBaseName . mp3SrcPath $ o

instance Encodable PodcastInput where
  encodeTitle pi o    = (datedString o) <> "-" <> (pcFilePrefix pi)
  encodeDestHifi pi o = (encodeTitle pi o) <> "-HIFI.mp3"
  encodeDest pi o     = (encodeTitle pi o) <> ".mp3"

data Opts = Opts { mp3SrcPath :: String
                 , bucket     :: String
                 , imagePath  :: String
                 , audioScale :: Int
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

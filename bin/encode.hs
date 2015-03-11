#!/usr/bin/env runhaskell

{-# LANGUAGE OverloadedStrings #-}
{-# LANGUAGE TemplateHaskell #-}

import Turtle
import Data.Yaml (decodeEither)
import Data.Aeson.TH ( deriveJSON
                     , defaultOptions
                     , fieldLabelModifier
                     , constructorTagModifier
                     )
import Data.Text (Text, unpack, pack)
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
                           , strOption
                           , long
                           , value)
import System.FilePath.Posix (takeBaseName)

defaultBucket = "media.trinitynashville.org"
defautlAuthor = "Trinity Church"

main :: IO ()
main = do
  o <- execParser $ info (helper <*> opts) (header "Trinity Audio Encoder")
  j <- getContents
  putStrLn $ case (decodeEither (B.pack j) :: Either String PodcastInput) of
    (Left err) -> "\n*** problem reading information file:\n\n" ++ err ++ "\n"
    (Right s) -> (unpack . pcComment $ s) ++ "\n" ++ (bucket o) ++ "\n" ++ (encodeTitle s o)

class Dated a where
  datedString :: a -> String

class Encodable a where
  encodeTitle :: Dated b => a -> b -> String

instance Dated Opts where
  datedString o = takeBaseName . mp3SrcPath $ o

instance Encodable PodcastInput where
  encodeTitle pi o = (datedString o) ++ "-" ++ (unpack . pcFilePrefix $ pi)

data Opts = Opts { mp3SrcPath :: String
                 , bucket :: String
                 }

opts :: Options.Parser Opts
opts = Opts <$> argument str (metavar "MP3")
       <*> strOption
           ( long "bucket"
           <> metavar "BUCKET"
           <> help "What bucket?"
           <> value defaultBucket )

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

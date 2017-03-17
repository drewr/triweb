module Main where

import Prelude (Unit)
import Data.Maybe
import Data.StrMap
import Control.Monad.Eff (Eff)
import Control.Monad.Eff.Console (CONSOLE, log)
import Node.ChildProcess

main :: forall t. Eff ( console :: CONSOLE | t ) Unit
main = do
  execFile "/bin/echo"
    ["bar"]
    { cwd: Just "."
    , env: Just (fromFoldable [])
    , timeout: Just 5
    }
    (\result -> log "foo")

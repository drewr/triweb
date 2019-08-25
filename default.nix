with import <nixpkgs> {};
stdenv.mkDerivation rec {
  name = "triweb";
  env = buildEnv { name = name; paths = buildInputs; };
  buildInputs = [
    awscli
    wget
    lame
    python3.7-eyeD3
  ];
}

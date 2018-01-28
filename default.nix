with import <nixpkgs> {};
stdenv.mkDerivation rec {
  name = "triweb";
  env = buildEnv { name = name; paths = buildInputs; };
  buildInputs = [
    awscli
    gmp
  ];
}

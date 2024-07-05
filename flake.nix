{
  inputs = {
    nixpkgs.url = "github:nixos/nixpkgs";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs = { self, nixpkgs, flake-utils, ... }@inputs:
    flake-utils.lib.eachDefaultSystem (
      system:
      let
        pkgs = import nixpkgs {
          inherit system;
        };

        dagger = pkgs.stdenv.mkDerivation {
          name = "dagger";
          src = builtins.fetchurl {
            url = "https://github.com/dagger/dagger/releases/download/v0.11.9/dagger_v0.11.9_darwin_arm64.tar.gz";
            sha256 = "19b32f83bwri49vipi4kkrgn9lxi6bbikqxjhcl4p8vk3ivvm38c";
          };
          dontUnpack = true;

          buildPhase = ''
            tar xzf $src
          '';

          installPhase = ''
            mkdir -p $out/bin
            cp dagger $out/bin
          '';
        };
      in
      {
        devShell = pkgs.mkShell {
          buildInputs = [
            pkgs.yarn
            pkgs.sbt
            dagger
          ];
        };
      }
    );
}

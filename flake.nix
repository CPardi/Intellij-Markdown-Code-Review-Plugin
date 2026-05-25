{
  description = "Java Template";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs = {
    nixpkgs,
    flake-utils,
    ...
  }:
    flake-utils.lib.eachDefaultSystem (
      system: let
        pkgs = nixpkgs.legacyPackages.${system};

        jdk = pkgs.javaPackages.compiler.openjdk21;

        fhsEnv = pkgs.buildFHSEnv {
          name = "intellij-dev-env";
          targetPkgs = pkgs: [
            jdk
            pkgs.jdt-language-server
            pkgs.stdenv.cc.cc.lib        # libstdc++, libc
            pkgs.libX11
            pkgs.libXext
            pkgs.libXrender
            pkgs.libXi
            pkgs.libXtst
            pkgs.libGL
            pkgs.fontconfig
            pkgs.freetype
            pkgs.gtk3
            pkgs.glib
            pkgs.zlib
          ];
          multiPkgs = pkgs: [
            pkgs.stdenv.cc.cc.lib
          ];
          runScript = "bash";
        };
      in {
        devShells.default = pkgs.mkShell {
          nativeBuildInputs = [
            jdk
            pkgs.jdt-language-server
          ];

          shellHook = ''
            echo "Run 'nix run .#fhs' to enter an FHS-compatible shell for 'gradlew runIde'"
          '';
        };

        apps.fhs = {
          type = "app";
          program = "${fhsEnv}/bin/intellij-dev-env";
        };
      }
    );
}

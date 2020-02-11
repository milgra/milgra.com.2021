with import <nixpkgs> {};

mkShell {
    buildInputs = [leiningen jdk overmind];
}

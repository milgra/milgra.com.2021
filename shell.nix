with import <nixpkgs> {};

mkShell {
    buildInputs = [
        nodejs (yarn.override { nodejs = nodejs; })
    ];
}

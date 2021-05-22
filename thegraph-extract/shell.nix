# shell.nix
let
  pkgs = import <nixpkgs> {};
  python = pkgs.python37;
  pythonPackages = python.pkgs;
in

with pkgs;

mkShell {
  name = "pip-env";
  buildInputs = with pythonPackages; [
    azure-cli
    kubectl

    # Python requirements (enough to get a virtualenv going).
    psutil
    #tensorflow
    #pyarrow
    pandas
    ipykernel
    jupyter
    pytest
    setuptools
    wheel
    venvShellHook

    libffi
    openssl
    gcc

    unzip
  ];
  venvDir = "venv37";
  src = null;
  postVenv = ''
    unset SOURCE_DATE_EPOCH
    ./scripts/install_local_packages.sh
  '';
  postShellHook = ''
    # Allow the use of wheels.
    unset SOURCE_DATE_EPOCH

    # get back nice looking PS1
    source ~/.bashrc
    source <(kubectl completion bash)

    PYTHONPATH=$PWD/$venvDir/${python.sitePackages}:$PYTHONPATH
  '';
}

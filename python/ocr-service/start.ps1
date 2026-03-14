$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$venv = Join-Path $root ".venv"
$env:PADDLE_PDX_CACHE_HOME = Join-Path $root ".cache\\paddlex"
$env:PADDLE_PDX_DISABLE_MODEL_SOURCE_CHECK = "True"

if (-not (Test-Path $venv)) {
    python -m venv $venv
}

$python = Join-Path $venv "Scripts\\python.exe"
& $python -m uvicorn app:app --host 0.0.0.0 --port 9001 --app-dir $root

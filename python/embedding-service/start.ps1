$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$venv = Join-Path $root ".venv"
$env:HF_HOME = Join-Path $root ".cache\\huggingface"
$env:TRANSFORMERS_CACHE = Join-Path $root ".cache\\transformers"
$env:SENTENCE_TRANSFORMERS_HOME = Join-Path $root ".cache\\sentence-transformers"

if (-not (Test-Path $venv)) {
    python -m venv $venv
}

$python = Join-Path $venv "Scripts\\python.exe"
& $python -m uvicorn app:app --host 0.0.0.0 --port 9002 --app-dir $root

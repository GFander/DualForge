#!/usr/bin/env zsh
# Unix entrypoint; JSON, ZIP evidence and process capture use Python's stdlib.
emulate -LR zsh
setopt errexit nounset pipefail
exec python3 -B "${0:A:h}/unix_workflow.py" build "$@"

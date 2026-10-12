#!/usr/bin/env zsh
emulate -LR zsh
setopt errexit nounset pipefail
exec python3 -B "${0:A:h:h}/lib/unix_workflow.py" dependencies "$@"

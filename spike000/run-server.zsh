#!/usr/bin/env zsh
# Only an explicitly requested isolated server run may launch Java.
emulate -LR zsh
setopt errexit nounset pipefail
exec python3 -B "${0:A:h:h}/tools/unix_workflow.py" harness "$@"

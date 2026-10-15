#!/usr/bin/env zsh
# Download only the ten pinned compile inputs, never install/start a server.
emulate -LR zsh
setopt errexit nounset pipefail
exec python3 -B "${0:A:h:h}/lib/prepare_unix.py" "$@"

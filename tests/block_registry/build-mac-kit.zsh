#!/usr/bin/env zsh
emulate -LR zsh
setopt errexit nounset pipefail
exec python3 -B "${0:A:h}/build_mac_kit.py" "$@"

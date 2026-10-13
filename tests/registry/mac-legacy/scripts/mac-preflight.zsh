#!/usr/bin/env zsh
emulate -LR zsh
setopt errexit nounset pipefail
DF_KIT="${0:A:h:h}"
source "${0:A:h}/mac-common.zsh"
instance='' java_path=''
while (( $# )); do
  case "$1" in
    --instance-dir) (( $# >= 2 )) || df_die 'Missing instance path.'; instance="$2"; shift 2 ;;
    --java-path) (( $# >= 2 )) || df_die 'Missing Java path.'; java_path="$2"; shift 2 ;;
    *) df_die 'Usage: mac-preflight.zsh --instance-dir <Prism instance> [--java-path <actual instance Java>]' ;;
  esac
done
df_platform
df_kit
df_prism
df_instance "$instance"
df_java "$java_path"
df_jvm
df_inventory
df_args
print -r -- 'Preflight PASS. This is environment/static validation, not client loading or rendering PASS.'

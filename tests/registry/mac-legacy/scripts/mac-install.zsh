#!/usr/bin/env zsh
emulate -LR zsh
setopt errexit nounset pipefail
DF_KIT="${0:A:h:h}"
source "${0:A:h}/mac-common.zsh"
mode='' instance='' phase=C java_path=''
while (( $# )); do
  case "$1" in
    --dry-run|--install|--rollback) [[ -z "$mode" ]] || df_die 'Choose one action.'; mode="$1"; shift ;;
    --instance-dir) (( $# >= 2 )) || df_die 'Missing instance path.'; instance="$2"; shift 2 ;;
    --phase) (( $# >= 2 )) || df_die 'Missing phase B/C.'; phase="$2"; shift 2 ;;
    --java-path) (( $# >= 2 )) || df_die 'Missing Java path.'; java_path="$2"; shift 2 ;;
    *) df_die 'Usage: --dry-run|--install|--rollback --instance-dir <path> [--phase B|C] [--java-path <path>]' ;;
  esac
done
[[ -n "$mode" && ( "$phase" == B || "$phase" == C ) ]] || df_die 'Choose an action and phase B or C. Client A has no mods.'
df_platform
df_kit
df_instance "$instance"
ledger="$DF_MODS/.dualforge-005-owned"
typeset -a owned desired
owned=()
if [[ -e "$ledger" ]]; then
  [[ -f "$ledger" && ! -L "$ledger" ]] || df_die 'Unsafe ownership ledger.'
  while IFS=$' \t' read -r name recorded_sha extra || [[ -n "$name" ]]; do
    [[ "$name" == forgecompat-002-0l.jar || "$name" == registry-probe.jar ]] || df_die 'Invalid ownership ledger.'
    [[ -z "$extra" ]] || df_die 'Unexpected ownership ledger fields.'
    # Filename-only ledgers from the first kit remain safe via the pinned kit SHA.
    [[ -z "$recorded_sha" || "$recorded_sha" == "$(df_sha "$DF_KIT/mods/$name")" ]] || df_die 'Ownership ledger cohort SHA mismatch.'
    owned+=("$name")
  done < "$ledger"
fi
if [[ "$mode" == --rollback ]]; then
  # Prevalidate the entire rollback; never remove changed or preexisting user files.
  for name in "${owned[@]}"; do
    df_no_links "$DF_MODS/$name"
    [[ ! -e "$DF_MODS/$name" || ( -f "$DF_MODS/$name" && "$(df_sha "$DF_MODS/$name")" == "$(df_sha "$DF_KIT/mods/$name")" ) ]] || df_die "Changed owned file $name; rollback refused without deleting anything."
  done
  for name in "${owned[@]}"; do [[ ! -e "$DF_MODS/$name" ]] || unlink "$DF_MODS/$name"; done
  [[ ! -e "$ledger" ]] || unlink "$ledger"
  print -r -- 'Rollback complete: only unchanged JARs recorded as newly installed by this script were removed.'
  exit 0
fi
df_prism
df_java "$java_path"
df_jvm
inventory="$(df_inventory)"
[[ "$phase" != B || ! -e "$DF_MODS/registry-probe.jar" ]] || df_die 'Client B must have compat only. Roll back owned files first.'
desired=(forgecompat-002-0l.jar)
[[ "$phase" != C ]] || desired+=(registry-probe.jar)
fingerprint="$(print -r -- "$DF_INSTANCE" "$phase" "$(df_sha "$DF_KIT/manifest.json")" "$inventory" | shasum -a 256 | cut -d ' ' -f 1)"
receipt="$DF_KIT/.state/$fingerprint.dryrun"
df_no_links "$DF_KIT/.state"
if [[ "$mode" == --dry-run ]]; then
  # The instance stays read-only. The kit stores an explicit dry-run receipt.
  mkdir -p "$DF_KIT/.state"
  [[ ! -L "$receipt" ]] || df_die 'Linked dry-run receipt refused.'
  print -r -- "$fingerprint" > "$receipt"
  print -r -- "Dry-run PASS phase=$phase. Would add only missing exact JARs under $DF_MODS. No instance files changed."
  df_args
  exit 0
fi
[[ -f "$receipt" && ! -L "$receipt" && "$(cat "$receipt")" == "$fingerprint" ]] || df_die 'Run --dry-run first for this exact instance/phase/cohort/current mods inventory.'
mkdir -p "$DF_MODS"
for name in "${desired[@]}"; do
  if [[ ! -e "$DF_MODS/$name" ]]; then
    # noclobber prevents replacing even a raced-in target; no user backup is needed.
    (setopt noclobber; cat "$DF_KIT/mods/$name" > "$DF_MODS/$name") || df_die "Copy refused: $name"
    owned+=("$name")
    for owned_name in "${owned[@]}"; do
      print -r -- "$owned_name $(df_sha "$DF_KIT/mods/$owned_name")"
    done > "$ledger"
    [[ "$(df_sha "$DF_MODS/$name")" == "$(df_sha "$DF_KIT/mods/$name")" ]] || df_die "Copy verification failed; retained for inspection: $name"
  fi
done
unlink "$receipt"
print -r -- "Install PASS phase=$phase; no existing file overwritten."
df_args

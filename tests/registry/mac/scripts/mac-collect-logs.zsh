#!/usr/bin/env zsh
emulate -LR zsh
setopt errexit nounset pipefail
umask 077
DF_KIT="${0:A:h:h}"
source "${0:A:h}/mac-common.zsh"
instance='' stage='' marker='' output_dir='' checklist="$DF_KIT/CHECKLIST.md"
typeset -a screenshots private_text
screenshots=(); private_text=()
while (( $# )); do
  (( $# >= 2 )) || df_die 'Every argument requires a value.'
  case "$1" in
    --instance-dir) instance="$2" ;;
    --stage) stage="$2" ;;
    --since-marker) marker="$2" ;;
    --output-dir) output_dir="$2" ;;
    --checklist) checklist="$2" ;;
    --screenshot) screenshots+=("$2") ;;
    --redact-text) [[ -n "$2" ]] || df_die 'Empty redaction text.'; private_text+=("$2") ;;
    *) df_die 'Usage: --instance-dir <path> --stage A|B|C --since-marker <file> --output-dir <existing directory> [--screenshot <chosen PNG>] [--redact-text <private hostname>]' ;;
  esac
  shift 2
done
[[ "$stage" == A || "$stage" == B || "$stage" == C ]] || df_die 'Choose stage A, B or C.'
df_platform
df_kit
df_instance "$instance"
[[ -f "$marker" && ! -L "$marker" ]] || df_die 'Before launching, create a stage marker with touch; pass that exact file.'
[[ -d "$output_dir" ]] || df_die 'Pass an existing user-selected output directory.'
df_no_links "$output_dir"
output_dir="${output_dir:A}"
[[ "$output_dir" != "$DF_INSTANCE" && "$output_dir" != "$DF_INSTANCE/"* ]] || df_die 'Diagnostics must be outside the instance.'
diagnostics="$output_dir/dualforge-005-client-$stage-$(date +%Y%m%d-%H%M%S)-$$"
mkdir "$diagnostics"
print -r -- "stage=$stage" "clientStatus=NOT_ASSESSED" "macOS=$(sw_vers -productVersion)" "CPU=$(uname -m)" \
  'Minecraft=1.21.1' 'NeoForge=21.1.256' > "$diagnostics/environment.txt"
java_path="$(df_cfg JavaPath)" || df_die 'No configured instance Java.'
[[ -x "$java_path" ]] || df_die 'Configured instance Java unavailable.'
"$java_path" -version > "$diagnostics/java-version.txt" 2>&1
for jar in "$DF_MODS"/*.jar(N); do
  [[ -f "$jar" && ! -L "$jar" ]] || df_die 'Linked/non-regular mod JAR refused.'
  print -r -- "${jar:t} $(df_sha "$jar")" >> "$diagnostics/installed-jars.txt"
done
[[ -f "$diagnostics/installed-jars.txt" ]] || print -r -- '(none)' > "$diagnostics/installed-jars.txt"
sanitize() {
  # Original files never change. Secret-bearing lines are omitted from the copy.
  perl -pe '
    if (/(?:token|authorization|bearer\s|password|passwd|cookie|private[_-]?key|api[_-]?key|session(?:id|\s+id|\s*=|\s*:)|--(?:username|uuid)|Setting user:)/i) { $_="[REDACTED sensitive-field line]\n"; }
    else {
      s{https?://[^\s"<>]+}{[REDACTED_URL]}gi;
      s{\b(?:\d{1,3}\.){3}\d{1,3}(?::\d+)?\b}{[REDACTED_IP]}g;
      s{\b(?:[0-9a-f]{1,4}:){5,7}[0-9a-f]{1,4}\b}{[REDACTED_IPV6]}gi;
      s{(?<![A-Za-z0-9])[0-9a-f:]*::[0-9a-f:]*(?![A-Za-z0-9])}{[REDACTED_IPV6]}gi;
      if (/(?:Connecting to|server address|server ip|hostname\s*[=:])/i) { $_="[REDACTED connection-address line]\n"; }
    }
  ' "$1" > "$2"
  local text
  for text in "${private_text[@]}"; do
    DF_PRIVATE_TEXT="$text" perl -i -pe 's/\Q$ENV{DF_PRIVATE_TEXT}\E/[REDACTED_USER_TEXT]/g' "$2"
  done
}
typeset -a logs
logs=()
for log in "$DF_GAME/logs/latest.log" "$DF_GAME/logs/debug.log"; do
  [[ ! -f "$log" || ! "$log" -nt "$marker" ]] || logs+=("$log")
done
for log in "$DF_GAME/crash-reports"/*.txt(N); do
  [[ ! "$log" -nt "$marker" ]] || logs+=("$log")
done
index=0
for log in "${logs[@]}"; do
  df_no_links "$log"
  index=$((index+1))
  sanitize "$log" "$diagnostics/log-$index-${log:t}"
done
[[ -f "$checklist" ]] || df_die 'Checklist file missing.'
df_no_links "$checklist"
sanitize "$checklist" "$diagnostics/CHECKLIST.md"
index=0
for screenshot in "${screenshots[@]}"; do
  [[ -f "$screenshot" && "${screenshot:e:l}" == png ]] || df_die 'Explicit screenshots must be existing PNG files.'
  df_no_links "$screenshot"
  [[ "$(head -c 8 "$screenshot" | od -An -tx1 | tr -d ' \n')" == 89504e470d0a1a0a ]] || df_die 'Selected screenshot is not a PNG.'
  index=$((index+1))
  cp -n "$screenshot" "$diagnostics/screenshot-$index.png"
done
print -r -- 'Local originals retained. Copies omit whole sensitive-field/connection lines and redact URLs/IPs plus --redact-text literals.' \
  'Necessary context may be hidden: report these omissions when returning diagnostics.' \
  'Screenshots are unchanged user-selected bytes: inspect for names, chat, private addresses and secrets before sharing.' \
  'No upload occurs. Inspect every archive member before deciding to return it.' > "$diagnostics/REDACTION-NOTES.txt"
(cd "$output_dir" && zip -qr "${diagnostics:t}.zip" "${diagnostics:t}")
unzip -tq "$diagnostics.zip" >/dev/null
print -r -- "Local diagnostic ZIP=$diagnostics.zip" "SHA256=$(df_sha "$diagnostics.zip")"
print -r -- "Collected ${#logs} current logs and ${#screenshots} explicit screenshots. No upload; no client PASS inferred."

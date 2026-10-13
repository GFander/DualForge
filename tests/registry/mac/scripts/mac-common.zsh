# Shared validation for the three Mac entrypoints; no launcher/global configuration writes.
df_die() { print -u2 -r -- "STOP: $*"; exit 1; }
df_sha() { shasum -a 256 "$1" | cut -d ' ' -f 1; }
df_no_links() {
  local candidate="${1:a}"
  while [[ "$candidate" != / ]]; do
    [[ ! -L "$candidate" ]] || df_die "Symlink path refused: $candidate"
    candidate="${candidate:h}"
  done
}
df_platform() {
  [[ "$(uname -s)" == Darwin ]] || df_die 'Run on macOS; Linux validation is not Mac execution.'
  DF_ARCH="$(uname -m)"
  [[ "$DF_ARCH" == arm64 || "$DF_ARCH" == x86_64 ]] || df_die "Unsupported CPU: $DF_ARCH"
  [[ "$(sysctl -in sysctl.proc_translated 2>/dev/null || true)" != 1 ]] || df_die 'Rosetta process detected; use native arm64 Prism, Terminal and Java 21.'
  print -r -- "macOS $(sw_vers -productVersion); process architecture=$DF_ARCH"
  for command in plutil shasum perl unzip zip; do
    command -v "$command" >/dev/null || df_die "Missing system utility: $command"
  done
}
df_kit() {
  [[ -f "$DF_KIT/manifest.json" && -f "$DF_KIT/SHA256SUMS" ]] || df_die 'Incomplete kit.'
  (cd "$DF_KIT" && shasum -a 256 -c SHA256SUMS) || df_die 'Kit SHA256SUMS mismatch.'
  local file
  for file in forgecompat-002-0l.jar registry-probe.jar; do
    [[ -f "$DF_KIT/mods/$file" && ! -L "$DF_KIT/mods/$file" ]] || df_die "Missing regular artifact: $file"
    unzip -tq "$DF_KIT/mods/$file" >/dev/null || df_die "Bad JAR: $file"
  done
  [[ "$(df_sha "$DF_KIT/mods/forgecompat-002-0l.jar")" == ed572ca9762e235e7d264355d35b3c9d3ab9ecf1da3f2c3234b1e28056aad752 ]] || df_die 'Wrong compat cohort.'
  [[ "$(df_sha "$DF_KIT/mods/registry-probe.jar")" == 312a4f697c1dc617ed48822c10179384b6559f4e6399db9210c15541c774132c ]] || df_die 'Wrong Forge cohort.'
}
df_cfg() {
  local key="$1" line
  while IFS= read -r line || [[ -n "$line" ]]; do
    line="${line%$'\r'}"
    if [[ "$line" == "$key="* ]]; then print -r -- "${line#*=}"; return 0; fi
  done < "$DF_INSTANCE/instance.cfg"
  return 1
}
df_instance() {
  [[ -n "$1" ]] || df_die 'Pass --instance-dir using Prism: Edit instance → Folder → parent of .minecraft.'
  df_no_links "$1"
  DF_INSTANCE="${1:A}"
  [[ -f "$DF_INSTANCE/mmc-pack.json" && -f "$DF_INSTANCE/instance.cfg" ]] || df_die 'Instance missing: manually create MC 1.21.1 + NeoForge 21.1.256 in Prism; do not guess a path.'
  [[ ! -L "$DF_INSTANCE/mmc-pack.json" && ! -L "$DF_INSTANCE/instance.cfg" ]] || df_die 'Linked instance metadata refused.'
  # Prism stores JSON. On the reported Mac, -lint accepts only plist syntax.
  plutil -p "$DF_INSTANCE/mmc-pack.json" >/dev/null 2>&1 || df_die 'Invalid mmc-pack.json.'
  local i=0 uid version mc='' neo=''
  while uid="$(plutil -extract "components.$i.uid" raw -o - "$DF_INSTANCE/mmc-pack.json" 2>/dev/null)"; do
    version="$(plutil -extract "components.$i.version" raw -o - "$DF_INSTANCE/mmc-pack.json")" || df_die 'Missing component version.'
    if [[ "$uid" == net.minecraft ]]; then [[ -z "$mc" ]] || df_die 'Duplicate Minecraft component.'; mc="$version"; fi
    if [[ "$uid" == net.neoforged ]]; then [[ -z "$neo" ]] || df_die 'Duplicate NeoForge component.'; neo="$version"; fi
    if [[ "$uid" == net.minecraftforge || "$uid" == net.fabricmc.fabric-loader || "$uid" == org.quiltmc.quilt-loader ]]; then df_die 'Other loader component refused.'; fi
    i=$((i + 1))
  done
  [[ "$mc" == 1.21.1 && "$neo" == 21.1.256 ]] || df_die "Requires MC 1.21.1 / NeoForge 21.1.256; found $mc / $neo. Fix manually in Prism."
  [[ ! (-d "$DF_INSTANCE/minecraft" && -d "$DF_INSTANCE/.minecraft") ]] || df_die 'Ambiguous game directories.'
  if [[ -d "$DF_INSTANCE/.minecraft" ]]; then DF_GAME="$DF_INSTANCE/.minecraft"
  elif [[ -d "$DF_INSTANCE/minecraft" ]]; then DF_GAME="$DF_INSTANCE/minecraft"
  else df_die 'No existing Prism game directory; launch Client A once first.'; fi
  df_no_links "$DF_GAME"
  DF_MODS="$DF_GAME/mods"
  df_no_links "$DF_MODS"
  [[ ! -e "$DF_MODS" || -d "$DF_MODS" ]] || df_die 'mods is not a directory.'
  print -r -- "Verified instance=$DF_INSTANCE; game=$DF_GAME; MC=$mc; NeoForge=$neo"
}
df_java() {
  local requested="${1:-}" configured override output arch major
  configured="$(df_cfg JavaPath)" || df_die 'Set an instance-specific Java path in Prism → Settings → Java.'
  override="$(df_cfg OverrideJavaLocation)" || df_die 'Enable the instance Java override in Prism.'
  [[ "$override" == true ]] || df_die 'Instance-specific Java override is not enabled.'
  [[ -z "$requested" || "$requested" == "$configured" ]] || df_die '--java-path differs from the actual instance JavaPath.'
  DF_JAVA="$configured"
  [[ -x "$DF_JAVA" ]] || df_die "Java executable missing: $DF_JAVA"
  output="$("$DF_JAVA" -XshowSettings:properties -version 2>&1)" || df_die 'Java inspection failed.'
  major="$(print -r -- "$output" | sed -nE 's/^[[:space:]]*java.version = ([0-9]+).*/\1/p')"
  arch="$(print -r -- "$output" | sed -nE 's/^[[:space:]]*os.arch = (.*)/\1/p')"
  [[ "$major" == 21 ]] || df_die "Requires Java 21; found $major. System default Java is unchanged."
  [[ ( "$DF_ARCH" == arm64 && ( "$arch" == aarch64 || "$arch" == arm64 ) ) || ( "$DF_ARCH" == x86_64 && ( "$arch" == x86_64 || "$arch" == amd64 ) ) ]] || df_die "Java CPU $arch does not match $DF_ARCH. Use a native Java 21 and native Prism process."
  print -r -- "Instance Java 21=$DF_JAVA; Java CPU=$arch"
}
df_prism() {
  local app
  for app in '/Applications/Prism Launcher.app' '/Applications/PrismLauncher.app' "$HOME/Applications/Prism Launcher.app" "$HOME/Applications/PrismLauncher.app"; do
    if [[ -d "$app" ]]; then print -r -- "Prism=$app"; return 0; fi
  done
  if command -v prismlauncher >/dev/null; then print -r -- "Prism=$(command -v prismlauncher)"; return 0; fi
  df_die 'Install Prism Launcher manually for your Mac CPU, then create the isolated exact-version instance.'
}
df_inventory() {
  local file name
  for file in "$DF_MODS"/*(DN); do
    name="${file:t}"
    [[ ! -L "$file" && -f "$file" ]] || df_die "Non-regular mods entry refused: $name"
    case "$name" in
      .dualforge-005-owned) continue ;;
      forgecompat-002-0l.jar|registry-probe.jar)
        [[ "$(df_sha "$file")" == "$(df_sha "$DF_KIT/mods/$name")" ]] || df_die "Same name, different SHA: $name; no overwrite."
        ;;
      *) df_die "Other mods entry found: $name. Use a fresh isolated instance; nothing is removed." ;;
    esac
    print -r -- "$name $(df_sha "$file")"
  done
}
df_args() { print -r -- 'Paste these JVM arguments in Prism instance Settings → Java (not the Minecraft command box):'; cat "$DF_KIT/JVM-ARGS.txt"; }
df_jvm() {
  local override arguments required value count key
  typeset -a tokens
  override="$(df_cfg OverrideJavaArgs)" || df_die 'Enable the instance Java arguments override in Prism.'
  [[ "$override" == true ]] || df_die 'Instance JVM arguments override is not enabled.'
  arguments="$(df_cfg JvmArgs)" || df_die 'No instance JvmArgs: paste JVM-ARGS.txt in Prism.'
  # Qt INI may quote the entire value and escape quotes inside that wrapper.
  # Decode one whole-value wrapper, then tokenize the actual JVM argument string.
  # A sequence of individually quoted arguments must retain its own grouping.
  tokens=("${(@)${(z)arguments}}")
  if [[ "$arguments" == \"*\" ]] && (( ${#tokens} == 1 )); then
    arguments="${(Q)arguments}"
  fi
  tokens=("${(@Q)${(z)arguments}}")
  while IFS= read -r required || [[ -n "$required" ]]; do
    [[ -n "$required" ]] || continue
    count=0
    key="${required%%=*}"
    for value in "${tokens[@]}"; do
      if [[ "${value%%=*}" == "$key" ]]; then
        count=$((count+1))
        [[ "$value" == "$required" ]] || df_die "Conflicting JVM property: $key"
      fi
    done
    [[ "$count" == 1 ]] || df_die "Required JVM argument must occur exactly once: $required"
  done < "$DF_KIT/JVM-ARGS.txt"
  df_no_links "$DF_GAME/dualforge-005-evidence/profile"
  print -r -- 'All required JVM arguments verified in the actual instance.cfg.'
}

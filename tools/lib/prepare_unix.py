"""Acquire fixed compilation artifacts through official HTTPS repositories."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import ssl
import urllib.error
import urllib.request

from unix_workflow import LIBRARIES, no_links

PINS = {
    "loader-4.0.45.jar": "8390398e67df23e72d85ed809685be2752c8ec545b7cbb6ad4b70fdc81e4dcd4",
    "modlauncher-11.0.5.jar": "fd9b9fc7cf043d2264ea0113eac6b258f0a71fece5b00e831bfbdf9726c78a24",
}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--server-input", type=Path, default=Path("/workspace/dualforge-cloud/server-base"))
    args = parser.parse_args()
    no_links(args.server_input.absolute())
    context = ssl.create_default_context(cafile=os.environ.get("SSL_CERT_FILE"))
    records = []
    for name in LIBRARIES:
        root = "https://repo.maven.apache.org/maven2/" if name.startswith("org/") else "https://maven.neoforged.net/releases/"
        url = root + name
        path = args.server_input / "libraries" / name
        algorithm, expected = "sha256", PINS.get(path.name)
        if expected is None:
            try:
                with urllib.request.urlopen(url + ".sha256", context=context, timeout=60) as response:
                    expected = response.read().decode().split()[0].lower()
            except urllib.error.HTTPError as error:
                if error.code != 404:
                    raise
                algorithm = "sha1"
                with urllib.request.urlopen(url + ".sha1", context=context, timeout=60) as response:
                    expected = response.read().decode().split()[0].lower()
        if len(expected) != (64 if algorithm == "sha256" else 40) or any(c not in "0123456789abcdef" for c in expected):
            raise RuntimeError("Invalid authoritative checksum: " + url)
        path.parent.mkdir(parents=True, exist_ok=True)
        if not path.exists() or hashlib.new(algorithm, path.read_bytes()).hexdigest() != expected:
            temporary = path.with_suffix(".jar.download")
            try:
                with urllib.request.urlopen(url, context=context, timeout=60) as response:
                    temporary.write_bytes(response.read())
                if hashlib.new(algorithm, temporary.read_bytes()).hexdigest() != expected:
                    raise RuntimeError("Checksum mismatch: " + url)
                temporary.replace(path)
            finally:
                temporary.unlink(missing_ok=True)
        actual = hashlib.sha256(path.read_bytes()).hexdigest()
        records.append({"path": name, "url": url, "sha256": actual,
                        "verificationAlgorithm": algorithm, "authoritativeChecksum": expected})
        print("VERIFIED " + name)
    manifest = args.server_input / "compile-inputs.json"
    manifest.write_text(json.dumps(records, indent=2) + "\n")
    print("Prepared eight compile JARs only; complete runtime and fixed Probe remain separate inputs.")


if __name__ == "__main__":
    main()

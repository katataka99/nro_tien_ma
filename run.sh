#!/usr/bin/env bash
# Foreground process: systemd owns restart, logging and shutdown.
set -euo pipefail
cd -- "$(dirname -- "${BASH_SOURCE[0]}")"
JAVA_BIN="${JAVA_BIN:-java}"
JAVA_XMS="${JAVA_XMS:-256m}"
JAVA_XMX="${JAVA_XMX:-2g}"
[[ -r data/config/config.properties ]] || { echo "Missing data/config/config.properties; copy the .example and configure MySQL." >&2; exit 1; }
if grep -Eq '^database.pass[[:space:]]*=[[:space:]]*CHANGE_ME[[:space:]]*$' data/config/config.properties; then
    echo "Set database.pass before starting." >&2
    exit 1
fi
[[ -r server.jar ]] || { echo "Missing server.jar; run python3 build.py." >&2; exit 1; }
command -v "$JAVA_BIN" >/dev/null || { echo "Java is not installed." >&2; exit 1; }
version=$("$JAVA_BIN" -version 2>&1 | head -n 1)
if [[ "$version" =~ \"([0-9]+) ]]; then
    (( BASH_REMATCH[1] >= 17 )) || { echo "Java 17+ required: $version" >&2; exit 1; }
else
    echo "Cannot determine Java version: $version" >&2
    exit 1
fi
# lib first: use the supplied Connector/J rather than the older embedded driver.
exec "$JAVA_BIN" "-Xms$JAVA_XMS" "-Xmx$JAVA_XMX" -Dfile.encoding=UTF-8 \
    -Duser.timezone=Asia/Ho_Chi_Minh -Djava.awt.headless=true \
    -cp 'lib/*:server.jar' server.ServerManager

#!/usr/bin/env bash
set -euo pipefail
[[ $EUID -eq 0 ]] || { echo "Run with sudo." >&2; exit 1; }
cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.."
[[ $(pwd -P) == /opt/nro_tien_ma ]] || { echo "Clone the repository to /opt/nro_tien_ma first." >&2; exit 1; }
[[ -r data/config/config.properties ]] || { echo "Configure data/config/config.properties first." >&2; exit 1; }
command -v systemctl >/dev/null || { echo "This installer requires systemd." >&2; exit 1; }
if ! id nro >/dev/null 2>&1; then
    useradd --system --user-group --home-dir /opt/nro_tien_ma --shell /usr/sbin/nologin nro
fi
# Game updates data/update_data and may write runtime files in the working directory.
chown root:nro /opt/nro_tien_ma
chmod 775 /opt/nro_tien_ma
chown -R nro:nro data
chmod 600 data/config/config.properties
install -m 644 deploy/nro-tien-ma.service /etc/systemd/system/nro-tien-ma.service
if [[ ! -e /etc/default/nro-tien-ma ]]; then
    java_bin=$(find /usr/lib/jvm -path '*/java-17-openjdk-*/bin/java' -type f -print -quit)
    [[ -n "$java_bin" ]] || java_bin=$(command -v java)
    printf 'JAVA_XMS=256m\nJAVA_XMX=2g\nJAVA_BIN=%s\n' "$java_bin" > /etc/default/nro-tien-ma
fi
systemctl daemon-reload
systemctl enable nro-tien-ma
printf 'Installed. Start with: systemctl start nro-tien-ma\nLogs: journalctl -u nro-tien-ma -f\n'

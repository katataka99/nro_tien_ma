#!/usr/bin/env bash
# Fresh installation only. Existing databases/users are never overwritten.
set -euo pipefail
[[ $EUID -eq 0 ]] || { echo "Run with sudo." >&2; exit 1; }
cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.."
[[ ! -e data/config/config.properties ]] || { echo "Config already exists. Follow README for upgrades." >&2; exit 1; }
read -r -p "Public IPv4 address or DNS name of this VPS: " game_host
[[ "$game_host" =~ ^[A-Za-z0-9][A-Za-z0-9.-]*$ ]] || { echo "Invalid IP/hostname." >&2; exit 1; }
command -v mysql >/dev/null
command -v python3 >/dev/null
mysql_admin=(mysql --protocol=socket -u root --batch --skip-column-names)
# Root socket authentication is the Ubuntu default. If it is unavailable, see README.
existing=$("${mysql_admin[@]}" -e "SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name='nro_tien_ma'; SELECT COUNT(*) FROM mysql.user WHERE user='nro_game';")
[[ "$existing" == $'0\n0' ]] || { echo "Database nro_tien_ma or user nro_game already exists. Nothing changed." >&2; exit 1; }
"${mysql_admin[@]}" --default-character-set=utf8mb4 < sql/new.sql
# Hex password avoids SQL and Java-properties escaping problems; never echo it.
db_password=$(python3 -c 'import secrets; print(secrets.token_hex(24))')
printf "CREATE USER 'nro_game'@'127.0.0.1' IDENTIFIED BY '%s'; GRANT SELECT, INSERT, UPDATE, DELETE ON nro_tien_ma.* TO 'nro_game'@'127.0.0.1';\n" "$db_password" | "${mysql_admin[@]}"
umask 077
GAME_HOST="$game_host" DB_PASSWORD="$db_password" python3 - <<'PY'
from pathlib import Path
import os
p = Path('data/config/config.properties')
s = Path('data/config/config.properties.example').read_text(encoding='utf-8')
s = s.replace('database.user=nro', 'database.user=nro_game')
s = s.replace('database.pass=CHANGE_ME', 'database.pass=' + os.environ['DB_PASSWORD'])
s = s.replace('Nro:127.0.0.1:14445', 'Nro:' + os.environ['GAME_HOST'] + ':14445')
with p.open('x', encoding='utf-8') as f:
    f.write(s)
PY
unset db_password
printf 'Database imported and config created. Keep data/config/config.properties private.\n'

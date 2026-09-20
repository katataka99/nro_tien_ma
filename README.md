# NRO Tien Ma — VPS Linux

Java 17+, MySQL, TCP **14445**. Repo chi chua server; client Unity build rieng.

## Cai moi tren Ubuntu 26.04 (RAM 8 GB)

Chay bang root hoac them sudo. Neu VPS da co MySQL/MariaDB, giu dich vu hien tai va bo qua buoc cai mysql-server.

```bash
apt update
apt install -y git python3 openjdk-17-jdk-headless mysql-client
# Chi cai neu VPS CHUA co MySQL/MariaDB:
apt install -y mysql-server

git clone https://github.com/katataka99/nro_tien_ma.git /opt/nro_tien_ma
cd /opt/nro_tien_ma
bash deploy/setup-database.sh
bash deploy/install-service.sh
systemctl start nro-tien-ma
journalctl -u nro-tien-ma -n 100 --no-pager
```

Setup database hoi IP public/domain VPS, import **sql/new.sql**, tao user `nro_game` voi mat khau ngau nhien va ghi config rieng. Script can `mysql -u root` dang nhap qua Unix socket. Neu root can mat khau, dung huong dan thu cong ben duoi. Script tu choi neu database `nro_tien_ma`, user `nro_game` hoac config da ton tai. Neu import loi, database co the da tao mot phan: kiem tra loi truoc khi thu lai.

Dump da co cot player.data_tutien va bang luyen dan. Khong import tat ca dump/migration lien tiep: migration la lich su cho database cu. Tai khoan/player da duoc loai khoi dump; cai moi khong co tai khoan dang nhap san.

Mo TCP 14445 trong firewall nha cung cap VPS. Neu UFW dang bat: `ufw allow 14445/tcp`. Giu MySQL trong mang rieng. Sua dia chi server trong client sang VPS.

## Quan ly

```bash
systemctl status nro-tien-ma --no-pager
journalctl -u nro-tien-ma -f
systemctl stop nro-tien-ma
systemctl restart nro-tien-ma
ss -ltnp 'sport = :14445'
```

systemd tu khoi dong sau reboot/thoat process; systemctl stop dung han. SIGTERM dong socket, luu player/clan/event va doi DB write dang chay toi da 60 giay. Service cho toi da 120 giay. Loi database/config tra exit code 1. Sau 5 lan loi trong 5 phut, sua nguyen nhan roi chay `systemctl reset-failed nro-tien-ma` va start lai.

Lich bao tri headless doc `server.autorestart`, `server.maintenance.hour` va `server.maintenance.min` trong `data/config/config.properties`. Khi bat, server thong bao truoc 60 giay, luu du lieu, thoat an toan; systemd khoi dong lai sau 10 giay. Gio chay theo `Asia/Ho_Chi_Minh` trong `run.sh`.

Heap mac dinh 256 MB–2 GB, chua tinh native memory/threads/MySQL. Sua `/etc/default/nro-tien-ma` (JAVA_BIN, JAVA_XMS, JAVA_XMX) roi restart. Installer chon Java 17 rieng neu co. Chi quan ly dich vu nro-tien-ma de khong anh huong cac game Java khac.

`bash run.sh` chay foreground de chan doan; khong chay dong thoi voi service tren cung port. Lich bao tri trong config hoat dong ca khi server chay headless.

## Database/config thu cong hoac database da co

Backup database truoc khi nang cap. Khong import dump cai moi len database co player.

Voi cai moi: `mysql -u root -p < sql/new.sql`, tao user rieng co quyen SELECT, INSERT, UPDATE, DELETE tren nro_tien_ma.*. Copy data/config/config.properties.example thanh data/config/config.properties, sua database.host/port/name/user/pass va server.sv1 (IP/domain VPS). Khong commit config that. Mat khau Java properties can escape backslash neu co.

Neu doi port, sua server.port, server.sv1, firewall va client. Sau khi config xong: `bash deploy/install-service.sh`.

## Build va cap nhat

JAR da build lai tu source. Build can JDK 17+ va Python 3:

```bash
python3 build.py
```

Build dung lib/ va dependencies dong goi san trong server.jar (network/EMTI); khong can NetBeans. Khong xoa JAR truoc khi build. Dau ra chi thay the sau khi compile thanh cong. Ant cung duoc ho tro neu co Python 3.

Tren Windows, neu server dang khoa JAR, build rieng bang `python build.py --output build/server-fixed.jar`, dung server truoc khi copy JAR moi vao server.jar.

## Sua loi cho dang nhap hang nghin giay

Ket noi MySQL cua game dat session time_zone=+07:00 de khop JDBC; thoi gian login/logout duoc ghi bang CURRENT_TIMESTAMP cua database. Khong doi timezone global cua MySQL hay Windows. Cach xu ly nay phu hop voi [huong dan Connector/J](https://dev.mysql.com/doc/connector-j/en/connector-j-time-instants.html).

Ban cu co the da ghi timestamp trong tuong lai. Sau khi dung server cu, chi reset tai khoan bi anh huong trong Navicat (thay TEN_TAI_KHOAN), roi khoi dong bang JAR moi:

```sql
UPDATE nro_tien_ma.account
SET last_time_login = NOW() - INTERVAL 1 MINUTE,
    last_time_logout = NOW() - INTERVAL 1 MINUTE
WHERE username = 'TEN_TAI_KHOAN';
```

Regression test `tests/LoginTimeZoneTest.java` dung bang TEMPORARY, can database thu nghiem rieng trong config. Da chay voi MySQL default UTC va JVM o UTC, Asia/Taipei, Asia/Ho_Chi_Minh; tat ca deu doc timestamp dung epoch.

Cap nhat JAR tu GitHub:

```bash
cd /opt/nro_tien_ma
systemctl stop nro-tien-ma
git pull --ff-only
systemctl start nro-tien-ma
```

Neu tu build, server.jar se la thay doi local: luu ban build rieng va xu ly thay doi truoc khi pull; giu nguyen config/database.

## Kiem tra

```bash
mkdir -p build/test-classes
javac --release 17 -proc:none -cp 'lib/*:server.jar' -d build/test-classes tests/AsyncFlushTest.java
java -cp 'build/test-classes:lib/*:server.jar' AsyncFlushTest
bash -n run.sh
bash -n deploy/install-service.sh
bash -n deploy/setup-database.sh
```

Da kiem tra compile Java 17, import SQL tren MySQL 8.0, server headless khoi dong/mo socket, EOF console va flush doi DB write dang chay. systemd tren VPS va ket noi client thuc te can xac nhan tren may dich.

Ubuntu packages: [Java 17](https://packages.ubuntu.com/resolute/openjdk-17-jdk), [MySQL](https://packages.ubuntu.com/resolute/mysql-server).

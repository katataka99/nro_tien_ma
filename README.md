# NRO Tien Ma server

Java game server. The bundled server.jar targets Java 17.

## VPS startup

1. Install Java 17 and MySQL.
2. Create the database and a dedicated database user. Review the SQL dumps and apply the migrations required by your version. Account/player data and runtime logs have been removed from these dumps.
3. Copy `data/config/config.properties.example` to `data/config/config.properties`; set database credentials and the public server address.
4. Allow inbound TCP port 14445 (or the configured port).
5. From the repository directory, run `bash run.sh` and inspect `logs/server.log`.

Keep MySQL private. Configure the client separately to connect to the VPS.

## Build status

The existing Ant build.xml references missing nbproject/build-impl.xml. Rebuilding from source requires restoring or replacing that build setup. The included JAR retains the original compiled code, with its embedded SQL dump sanitized; its correspondence to the latest source and successful startup have not been verified.

This repository is a clean export. Local database credentials, IDE files, build output folders, and the original Git history are not included.

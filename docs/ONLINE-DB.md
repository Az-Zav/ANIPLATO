# Shared online database (prototype)

One hosted MySQL (Aiven) replaces everyone's local MySQL.

**Owner, once:** `.\scripts\setup-online-db.ps1 -DbHost <host> -Port <port> -CaFile <ca.pem> -LoadSchema -LoadSeed`
(creates `aniplato`, the data-only user `aniplato_app`, and `aniplato_test_user`; refuses if `aniplato` already has tables).

**Each teammate:** copy `src/main/resources/db.properties.online.example` to `db.properties`, fill in host, port, password and the
path to the CA file (download it from the service page; keep it outside the repo).

**Tests never touch the shared data.** `mvn test` drops and recreates a database named `aniplato_test` (or the name in
`ANIPLATO_TEST_DB`, which must start with `aniplato_test`). On the shared server give each person their own, so runs do not collide:

    $env:ANIPLATO_TEST_DB = "aniplato_test_<yourname>"
    $env:ANIPLATO_TEST_DB_USER = "aniplato_test_user"
    $env:ANIPLATO_TEST_DB_PASSWORD = "<test user password>"

Do not run `sql/schema.sql` against the shared database: it drops `aniplato`.

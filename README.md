# ANIPLATO

Surplus-food board for Metro Manila: donors post surplus, food groups offer to collect it, admins verify organizations. JavaFX + MySQL, OOP finals prototype.

## Setup
1. Install JDK 21+, Maven, and MySQL 8+ (the Maven dependency is only the JDBC driver, not the server). Make sure the MySQL service is running.
2. Copy `src/main/resources/db.properties.example` to `db.properties` and set your MySQL password.
3. Run `setup-db.bat` (or `./setup-db.ps1`). It creates the `aniplato` database and loads demo data. It asks before replacing an existing database; `-Force` skips the question.
4. `mvn javafx:run`
5. `mvn test` runs the tests on their own `aniplato_test` database (see `AGENTS.md`). Without MySQL, use `-DskipTests`.

Demo logins, password `Aniplato123!` for all: `admin@aniplato.test`, `business@aniplato.test`, `casual@aniplato.test`, `group@aniplato.test`. `pending@aniplato.test` is a registration waiting for the admin, not a login.

## Layout
```
src/main/java/com/aniplato/
  Main.java
  model/       entities and enums
  view/        FXML + CSS (admin/, donor/, foodgroup/, shared/, shared/dialog/)
  controller/  one per screen (admin/, donor/, foodgroup/, shared/); shared/dialog/ has one per popup
  service/     business rules, one transaction per action
  dao/         SQL only, one per table
  exception/   RuleViolationException
  util/        AppConfig, Database, PasswordHasher, Settings, Session, Navigator
src/test/      tests (service rules, FXML wiring, popups, architecture rules)
sql/           schema.sql, seed.sql
scripts/       new-methods.sh (lists the methods a branch adds, for review)
docs/          MAP.md (what exists), FLOWS.md (what to build), ARCHITECTURE.md (schema + diagrams)
uploads/       documents and photos (git-ignored; folder set by uploads.dir in db.properties)
```
Start with `AGENTS.md` (how to add code), then `docs/MAP.md`. `CLAUDE.md` has the coding rules and `docs/Project Spec.md` the full spec.

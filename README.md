# ANIPLATO

Surplus-food board for Metro Manila: donors post surplus, food groups offer to collect it, admins verify organizations. JavaFX + MySQL, OOP finals prototype.

## Setup
1. Install JDK 21+, Maven, and MySQL 8+ (the Maven dependency is only the JDBC driver, not the server). Make sure the MySQL service is running.
2. Copy `src/main/resources/db.properties.example` to `db.properties` and set your MySQL password.
3. Run `setup-db.bat` (or `./setup-db.ps1`). It creates the `aniplato` database and loads demo data. It asks before replacing an existing database; `-Force` skips the question.
4. `mvn javafx:run`

Demo logins, password `Aniplato123!` for all: `admin@aniplato.test`, `donor@aniplato.test`, `group@aniplato.test`.

## Layout
```
src/main/java/com/aniplato/
  Main.java
  model/       entities and enums
  view/        FXML + CSS (admin/, donor/, foodgroup/, shared/)
  controller/  one per screen (admin/, donor/, foodgroup/, shared/)
  service/     business rules, one transaction per action
  dao/         SQL only, one per table
  exception/   RuleViolationException
  util/        Database, PasswordHasher, Settings, Session, Navigator
sql/           schema.sql, seed.sql
docs/          ARCHITECTURE.md (schema + diagrams)
uploads/       documents and photos (git-ignored)
```
See `CLAUDE.md` for the coding rules and `docs/Project Spec.md` for the full spec.

# ANIPLATO

Surplus-food board for Metro Manila: donors post surplus, food groups offer to collect it, admins verify organizations. JavaFX + MySQL, OOP finals prototype.

## Setup
1. Install JDK 17+, Maven, MySQL 8.
2. `mysql -u root -p < sql/schema.sql` then `sql/seed.sql`.
3. Copy `src/main/resources/db.properties.example` to `db.properties` and set your password.
4. `mvn javafx:run`

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
See `.clinerules` for the coding rules.

# ANIPLATO

Surplus-food board for Metro Manila (donors post, food groups offer, admin verifies). JavaFX 21 + MySQL over plain JDBC, Maven, package `com.aniplato`. OOP finals prototype.

## Source of truth
- `docs/Project Spec.md` is the PRD: flows (section 6), architecture and rules A1-A9 (section 7), data model (7.6). Rule IDs (R1.1, A2...) go in code comments and tests. If code and spec disagree, the spec wins; if the spec looks wrong, ask.
- `sql/schema.sql` is the only copy of the schema. `docs/ARCHITECTURE.md` holds the diagrams.

## Commands
- Run: `mvn javafx:run` (main class `com.aniplato.Main`)
- Build: `mvn clean compile` / `mvn clean package`
- Database: `mysql -u root -p < sql/schema.sql`, then `sql/seed.sql`. schema.sql drops and recreates `aniplato`; never run it against data you want to keep.
- Config: `src/main/resources/db.properties` (git-ignored; copy from `db.properties.example`).

## Layering (View → Controller → Service → DAO → MySQL)
- Controllers are thin: read input, call a service, show the result. They never call DAOs and hold no rules (A1).
- Services own every rule and every status change, and write the status-change log (A3). An action touching more than one table is one service method and one transaction (A2).
- DAOs hold SQL only, one per table. Use `PreparedStatement`; never concatenate input into SQL.
- Rule failures throw `RuleViolationException` and are shown as a message (A8).
- Shared screens never check the user's role (A5). Phone numbers come from one method only (A7).
- `MaintenanceService` changes data only by calling other services (A4).
- Store statuses, timestamps and entered facts; derive rates, totals and warnings (A9).

## Conventions
- Match the existing package layout in README.md; one controller per FXML screen, FXML/CSS live in `src/main/java/.../view/` (the pom copies them to the build output).
- Only `Navigator` knows view paths. Only `Database` opens connections.
- Navigation-only buttons use a `nav-<screen>` style class (no handler); buttons that do logic use `onAction` and call `Navigator.show`. Wiring points are tagged `PATTERN:NAV|ACTION|DYNAMIC|LIST`; unfinished logic is `TODO(service)`. See `docs/FXML-GUIDE.md`.
- Settings values live in `util/Settings`, not scattered literals.
- Schema change = update `sql/schema.sql`, the spec's 7.6 data model, and the ER diagram together.

## Do not commit
`db.properties`, `uploads/*` (keep `.gitkeep`), `target/`.

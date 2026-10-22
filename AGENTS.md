# AGENTS.md: read this before writing any code

For every AI assistant and every human on this project. `CLAUDE.md` has the rule list (A1-A9); `docs/Project Spec.md` is the source of truth (if code and spec disagree, the spec wins). This file says how to add code without making a mess.

## Start of every session
1. Read this file, `CLAUDE.md`, **`docs/MAP.md`** (what exists) and your module in **`docs/FLOWS.md`** (what to build, under "Blueprints").
2. **Search before you write.** `grep` for an existing method or class that does the job. Duplicates were our biggest problem.
3. Work on **one module**. Touch the fewest files. Do not edit another person's module.
4. Before writing, list the files you will touch and the existing methods you will reuse, then wait for the human's OK.

## Where does this code go?
| It is... | It goes in... |
|---|---|
| SQL | `dao/` (one per table; takes a `Connection`; no rules) |
| A rule, a validation, a status change, a log row, a transaction | `service/` |
| Reading fields, calling ONE service, showing text | `controller/` |
| A number, limit or text that may change | `util/Settings` |
| A screen's layout | `view/*.fxml` + css |
| A popup | its own card FXML in `view/shared/dialog/` + a `DialogCard` subclass in `controller/shared/dialog/`, one `fx:include` in `dialog.fxml`, one `showXxxMode` in `DialogController` |

Flow is always **View → Controller → Service → DAO → MySQL**. Never skip a layer.

## Recipe: adding a feature (in this order)
1. Find the rule in the spec (section 6) and note its ID (R1.1, A2…). Open your blueprint in `docs/FLOWS.md`.
2. **DAO:** the SQL method. `PreparedStatement` only.
3. **Service:** ONE public method per user action: check the rules, `Database.inTransaction("message", c -> {...})` for anything that writes, call `LogDao.logStatusChange` for every status change, throw `RuleViolationException` for a broken rule.
4. **Controller:** read input, call that method through `FormSupport.attempt` (it catches `RuleViolationException` and shows the message), then navigate. List screens fill a `VBox` with `Cards` and `PostCards` (see `MyPostRows` for the pattern); forms use `DateTimeField` and `PhotoPicker`.
5. **Test:** copy the pattern in `src/test/java/com/aniplato/service/AccountStatusTest.java`. One test per rule in the blueprint's "Done when".
6. Run `mvn test`, then `scripts/new-methods.sh` and check every method it lists is in your blueprint.

## Tests
- `mvn test` needs MySQL running. Tests use their own **`aniplato_test`** database, rebuilt from `sql/schema.sql` and `sql/seed.sql` before every test, and refuse any other database. They read the MySQL user and password from your `db.properties` (or `ANIPLATO_TEST_DB_USER` / `ANIPLATO_TEST_DB_PASSWORD`). They never send real email and never touch `uploads/`.
- Skip them with `mvn package -DskipTests` if you have no MySQL.
- `MapCoverageTest` fails when a file, or a public method of a service, DAO, util or model class, has no row in `docs/MAP.md`: add the row in the same pull request.
- `ScreensTest` loads the round 3 screens against the seeded test database; copy it when you wire another screen (it needs a desktop).
- `ArchitectureTest` fails the build on a layer breach or a class over 300 lines. `FxmlWiringTest` fails when an FXML and its controller do not match. `DialogControllerTest` drives the real popups (needs a desktop).
- Tests assume today's seed data. If you change `sql/seed.sql`, update the tests that depend on it in the same pull request.

## Hard limits (stop and ask the team before breaking one)
- A class over **300 lines** is too big. Split it instead of adding to it (`ArchitectureTest` enforces this; a listed exception may not grow).
- A method over **40 lines** or **3 levels** of nesting: split it.
- Controllers never import `dao.*` and hold no rules, SQL or `if (role == …)` (A1, A5).
- Transactions: only `Database.inTransaction`. Never write `setAutoCommit`, `commit` or `rollback`.
- No new "helper", "util" or "manager" class without checking `util/` and the team first.
- No literals for values in `Settings`. No unused code, commented-out code or "temporary" classes.
- Schema change = `sql/schema.sql` + spec 7.6 + the ER diagram + the seed and tests that depend on it, together.
- A shared file (`Navigator`, `Settings`, `Database`, `AppConfig`, `DialogController`, `DialogCard`, `schema.sql`) changes only in a pull request someone else reviews.

## Conventions (the full list is C1-C9 at the top of `docs/FLOWS.md`)
- Service names are verb phrases (`sendOffer`); DAO names are `findX` / `insert` / `updateX`.
- One service calling another inside a transaction passes its `Connection` (see `AccountStatusService.suspend`).
- A rule ID goes in a comment only where the rule is enforced.
- The uploads folder is `uploads.dir` in `db.properties` (default `uploads/`); open stored files with `FileStorageService.resolve`, never `new File("uploads", …)`.

## Status
Every module of `docs/FLOWS.md` is built (see `docs/MAP.md`) except the AI assistant, which is a stretch in the spec. `setup-db -Demo` loads the demo story; the README has the demo script. What remains is review, polish and the open questions in `docs/CLEANUP-REPORT.md`.

## Ownership (write the names; one owner per slice)
| Slice (FLOWS.md modules) | Owner |
|---|---|
| 4 Profile and renewals · 13 polish | ? |
| 5 Post a listing · 9 Requests | ? |
| 6 Browse and offer · 7 Accept and decline | ? |
| 8 Handoff and cancel · 10 Report and safety | ? |
| 11 Dashboards, History · 12 Maintenance | ? |

## Before you push
- [ ] `mvn test` passes (or you said why it cannot run).
- [ ] I searched for an existing method before adding one.
- [ ] Every method `scripts/new-methods.sh` lists is in my blueprint, or I explain it in the pull request.
- [ ] Every file and public method I added has a row in `docs/MAP.md`.
- [ ] I can explain my service method line by line, including what happens when step 3 fails.
- [ ] My pull request does one thing and names the spec rule or module it implements.

## When the AI is unsure
Ask the human. Do not invent a new pattern. Match the closest existing file.

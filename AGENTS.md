# AGENTS.md — read this before writing any code

For every AI assistant and every human on this project. `CLAUDE.md` has the full rule list (A1-A9);
`docs/Project Spec.md` is the source of truth. If code and spec disagree, the spec wins.

## Start of every session
1. Read `AGENTS.md`, `CLAUDE.md`, and the part of `docs/Project Spec.md` for your feature.
2. **Search before you write.** `grep` for an existing method, helper or class doing the same thing.
   Duplicates are our #1 problem (three copies of `inTransaction`, two `MakeOfferController`s).
3. Work on **one feature slice** (see Ownership). Touch the fewest files possible.

## Where does this code go?
| It is...                                         | It goes in...            |
|--------------------------------------------------|--------------------------|
| SQL                                              | `dao/` (one per table)   |
| A rule, a validation, a status change, a log row | `service/`               |
| Reading fields, calling ONE service, showing text| `controller/`            |
| A number/limit/text that may change              | `util/Settings`          |
| A screen's layout                                | `view/*.fxml` + css      |

Flow is always **View -> Controller -> Service -> DAO -> MySQL**. Never skip a layer.

## Recipe: adding a feature (do these in order)
1. Find the rule in the spec (section 6) and note its ID (R1.1, A2...).
2. DAO: add the SQL method. `PreparedStatement` only, takes a `Connection`, no rules.
3. Service: add ONE public method that checks the rules, changes data, writes the status log,
   and throws `RuleViolationException` on a broken rule. Several tables = one method = one transaction.
4. Controller: read input, call that method, catch `RuleViolationException`, show the message.
5. Run the app and click through it. Add a test if the method has a rule in it.

## Hard limits (stop and ask the team if you need to break one)
- A class over **300 lines** is too big. Split it instead of adding to it.
- A method over **40 lines** or more than **3 levels of nesting**: split it.
- Controllers never import `dao.*` and never contain rules, SQL or `if (role == ...)` logic (A1, A5).
- **Do not add to `DialogController`.** New popups get their own small controller + FXML.
- No new "helper"/"util"/"manager" classes without checking `util/` and the team first.
- Transactions: use the shared helper once it exists (`util/Transactions`); never write another
  `setAutoCommit/commit/rollback` block.
- No literals for settings values. No unused code, commented-out code, or "temporary" classes.
- Do not edit another person's slice or a shared file (`Navigator`, `Settings`, `Database`,
  `DialogController`, `schema.sql`) without a pull request that someone else reviews.
- Schema change = `sql/schema.sql` + spec 7.6 + ER diagram together.

## Ownership (edit the names; one owner per slice)
| Slice                                   | Owner |
|-----------------------------------------|-------|
| Login, registration, profile, passwords | ?     |
| Admin: review, accounts, safety, reports| ?     |
| Donor listings                          | ?     |
| Food-group requests and offers          | ?     |
| Handoff, reports, stats, history        | ?     |

## Before you push
- [ ] `mvn clean compile` passes and I ran the screen I changed.
- [ ] I searched for an existing method before adding a new one.
- [ ] No controller touches a DAO; no file I touched is over 300 lines.
- [ ] My pull request changes one thing and says which spec rule it implements.
- [ ] I reviewed my own diff and deleted anything I did not need.

## When the AI is unsure
Ask the human. Do not invent a new pattern. Match the closest existing file.

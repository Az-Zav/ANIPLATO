# Cleanup report

Branch `ccr-f2e57831-6tfkkw`. Round 1 is 12 commits after the tag **`pre-cleanup`** (the code as it was); round 2 is 14 more after the tag **`cleanup-round-1`**, described next. The rule was: **behavior-preserving**, no new features, bugs found are written down and not fixed.

**To undo everything:** `git checkout pre-cleanup`. **To undo one step:** `git revert <commit>` using the table below.

## Round 2: gaps closed and the map

Behavior-preserving again, except where a row says otherwise. One commit per step, the full suite green before each. No commit carries an attribution line.

| # | Commit | Step | Behavior |
|---|---|---|---|
| 1 | b3de28f | Restore `MaintenanceService` (the spec requires it); only the expiry check exists today | unchanged |
| 2 | 53a32f2 | The 6.6 checks now run before each screen loads (`Navigator` before-load hook, registered by `ShellController`); a failing check is printed and skipped | **changes**: an account with an expired verified document is suspended on the next navigation |
| 3 | db7429e | Spec 6.3: the admin reinstates, no written reason | docs only |
| 4 | 4e10778 | Removed `listingCountLabel` and `notesArea` (no screen had them); `FxmlWiringTest` has no known gaps | unchanged |
| 5 | 9027048 | Deleted the empty shared Listing form and offers screens | unchanged |
| 6 | 477fc42 | Deleted five unused methods; `canPostOrOffer` kept and tested | unchanged |
| 7 | 99f49c7 | The resolved-notice popup is the 16th dialog card | same text, styled card |
| 8 | 6f69cb6 | `DocumentUpload` moved to `model/` | unchanged |
| 9 | 9c7000c | The notice `kind` is written explicitly | unchanged |
| 10 | a82b146 | The login screen's Forgot password link works | **changes**: the link now works |
| 11 | 910dded | `UploadSlot` split out of `RegisterStep2Controller` (342 to 278 lines) | unchanged |
| 12 | cc3a2b9 | Fake file names removed from the upgrade dialog | unchanged |
| 13 | f076756 | `MAP.md` rewritten as a feature and file map, with `MapCoverageTest` | docs only |
| 14 | this commit | Mermaid diagrams in `MAP.md` and `FLOWS.md` (all 20 parse with the real Mermaid parser), this report | docs only |

`ArchitectureTest` has no exception lists any more, and `FxmlWiringTest` no known gaps.

**Check by hand after pulling** (no test drives these): register an organization through both steps (the upload rows moved), and log in as each role and open two screens (the shell now runs the checks first).

## Result

| | Before | After |
|---|---|---|
| Biggest service | `RegistrationService` 458 lines, 29 methods | 286 lines; the rest moved to two services of 75 and 127 lines |
| Biggest popup class | `DialogController` 550 lines, 44 methods | 209 lines (one-line methods), plus 15 dialog controllers of 20 to 45 lines each |
| `dialog.fxml` | 699 lines, 15 popups in one file | 33 lines that include 15 files of 36 to 85 lines |
| Copies of the commit/rollback code | 4 | 1 (`Database.inTransaction`) |
| Copies of the "admin only" check | 3 | 1 (`Session.requireAdmin`) |
| Code on the removed `listing` table | 5 files | deleted |
| Tests | 0 | **115, all passing** |
| Java files / lines in `src/main` | 76 files / 6,893 lines | 90 files / 6,709 lines |

## Steps

| # | Step | Commit | Status |
|---|---|---|---|
| 1 | `AppConfig` and an `uploads.dir` setting (shared uploads folder works); the two screens that open files use `FileStorageService.resolve` | e2e8ade | done |
| 2 | JUnit 5, the `aniplato_test` database, seed loader (refuses any other database name) | 372ed83 | done |
| 3 | Safety-net tests for the behavior as it was (78) | dd5a0c0 | done |
| 4 | One transaction helper, `Database.inTransaction` | 81988d3 | done |
| 5 | One admin check, `Session.requireAdmin` | 4660eb2 | done |
| 6 | `RegistrationService` split into three services | 6cc9637 | done |
| 7 | Stale `listing`/offer code removed | b34b89a | done |
| 8 | A test that drives the real popups (26 then) | ff3a9e2 | done |
| 9 | `DialogController` and `dialog.fxml` split, one FXML and controller per dialog | 0d281de | done |
| 10 | `DevPreview` deleted | 7019280 | done |
| 11 | Architecture guard tests and `scripts/new-methods.sh` | 9cf3579 | done |
| 12 | `docs/MAP.md`, blueprints in `docs/FLOWS.md`, `AGENTS.md`, README, this report | next commit | done |

Nothing was reverted or skipped. Merging the "duplicate" controllers was **not** done on purpose: the donor and food group `MakeOfferController`s and the two `MyOffers` controllers are different forms for different routes, not copies (FLOWS.md module 6).

## Tests: 136 run, 0 failed, 0 skipped

| Test class | Tests | What it proves |
|---|---|---|
| AuthenticationServiceTest | 13 | login, wrong password message, temporary password flag, change password rules, forgot password |
| RegistrationApplyTest | 14 | the details form, the documents per role, expiry and file rules, what `submit` saves, nothing saved when a rule fails |
| RegistrationDecisionTest | 14 | approve (account, log, upgrade, rollback) and reject, only once, admin only, the review queue |
| DocumentReviewTest | 7 | verify, undo, the Profile document list and expiry banner |
| AccountStatusTest | 9 | suspend, reinstate, the All Accounts list, the document-expiry suspension |
| SafetyServiceTest | 10 | raise, dismiss, suspend the flagged side, admin only |
| StatsServiceTest | 7 | dashboard numbers, rescue rate, monthly report, null rates |
| DialogControllerTest | 30 | the real popups: text, one open at a time, Back, callbacks, three host screens, the new dialog |
| FxmlWiringTest | 5 | every handler and `@FXML` field matches its FXML |
| ArchitectureTest | 9 | layer rules (no exceptions left) and the 300-line limit |
| TestDbTest | 1 | the test database loads |
| MapCoverageTest | 3 | docs/MAP.md lists every file and every public method, and names no missing file |
| MaintenanceServiceTest | 3 | the expiry check runs once and is quiet when nothing expired |
| ShellMaintenanceTest | 3 | the shell runs the 6.6 checks before each screen, not on the login screen |
| LoginScreenTest | 1 | the Forgot password link opens the dialog with the typed email |
| UploadSlotTest | 5 | one upload row shows, restores and reports its file |
| SafetyNoticeDaoTest | 1 | the notice kind is written and read back |
| AccountTest | 1 | a Suspended account may not post or offer |

**Run them:** `mvn test` (MySQL running; reads your `db.properties` login). Takes about 90 seconds.
**How I know they can fail:** I added a deliberately bad file and `ArchitectureTest` failed on three rules, then removed it. The registration and dialog tests passed unchanged across the splits, which is what they were written for.

## Behavior that changed (only these)

1. **`SafetyService.raise` now works.** It read the removed listing table and would have thrown. Deleting the stale code forced me to read the offer through `OfferDao.findParties` and a new `findStatus`. It is covered by tests.
2. **`DevPreview` is gone**, so `ANIPLATO_PREVIEW` no longer skips the login.
3. **Deleted:** `ListingService`, `ListingDao`, `Listing`, `OfferService`, `MaintenanceService` (all on the removed `listing` table; no working screen called them). `OfferDao` lost six queries on columns that no longer exist; `Offer` keeps only its `Status` enum. They return in FLOWS.md modules 5, 6, 8 and 12.
4. **The popups:** same public methods, same texts. Two null checks that could never be true were dropped (Forgot Password and Upgrade dialogs).
5. **Uploads folder** is now read from `uploads.dir` (default `uploads`, so nothing changes unless you set it).

## Bugs and gaps found in round 1, and what became of them

| # | Where | What | Status |
|---|---|---|---|
| 1 | `LoginController` | The Forgot password link on the login screen was not wired | **closed** in round 2 (a82b146) |
| 2 | `MyListingsController`, `NewListingController` | two `@FXML` fields with no `fx:id` in their FXML | **closed** (4e10778): both fields were leftovers and are gone |
| 3 | `AccountStatusService.reinstateAccount` | spec 6.3 asked for a written reason; the code never had one | **closed** (db7429e): the spec now says the admin reinstates |
| 4 | `DocumentReviewService.verifyDocument` | spec 6.3: a verified renewal reinstates an account suspended for "Document expired" | open: FLOWS.md module 4 |
| 5 | `SafetyNoticesController` | built its own `Alert` | **closed** (99f49c7): a dialog card |
| 6 | `util/RegistrationDraft` | a util importing a service record | **closed** (6f69cb6): `DocumentUpload` is in `model/` |
| 7 | `RegisterStep2Controller` | 342 lines | **closed** (910dded): `UploadSlot`, now 278 lines |
| 8 | `UpgradeBusinessDonorDialogController` | fake file names in the label | **closed** (cc3a2b9): honest placeholders; the real picker is module 4 |
| 9 | `SafetyNoticeDao.insert` | left out `kind` | **closed** (9c7000c); the overdue notice (module 10) still needs an insert that accepts no raiser |
| 10 | Eight dialogs | open and close only until their module is built | open: modules 4 to 10 |
| 11 | `docs/FLOWS.md`, `README.md` (old text) | wrong statements about `kind` and a demo login | corrected in round 1 |
| 12 | `MaintenanceService` | deleted in round 1 although the spec requires it | **closed** (b3de28f, 53a32f2): restored and attached |
| 13 | `AuthenticationService.mustChangePassword` | no caller; the login screen reads the Account field | open: candidate for deletion, flagged in MAP.md section 6 |
| 14 | `ReportService` | an empty scaffold class with a wrong comment | comment fixed (f076756); built in module 10 |

## Not covered by tests (check by hand, 5 minutes)

The tests call services and load the popups, but nobody drives the real screens end to end. Before you trust a merge, click through once:

1. Log in as `admin@aniplato.test` → Pending → open Ana Cruz → Verify the document → Approve (and a second time: Reject with a reason).
2. Accounts → Suspend an account with a reason → Reinstate it.
3. Safety Notices and Reports open without an error.
4. Log in as `business@aniplato.test` → Profile → the documents list opens a file.
5. Register a new organization through both steps.

Also not covered: how the screens look, the real email (the tests use no SMTP login), the file chooser, and every host screen other than login, register and register-step2.

## Using a shared database with the team

- Each person sets `url=` in their own git-ignored `db.properties` to the host's address; no code change. Use a private network (Tailscale, ZeroTier), not a public tunnel, and a MySQL user that is not `root` with no password.
- **Only one person runs `setup-db`**: `schema.sql` drops the database.
- Uploaded files are not in the database. Point everyone's `uploads.dir` at one synced folder, or each person will see "file is not in the uploads folder" for documents registered on another machine.
- Real emails go out from whoever's SMTP login is in their `db.properties`: use dummy addresses in test registrations.
- `mvn test` never uses the shared database; it builds `aniplato_test` on whatever MySQL your `db.properties` points to, so run it against a local MySQL, or make sure that server's user may create databases.

## Open questions for the group

1. Delete `AuthenticationService.mustChangePassword` (gap 13)? Nothing calls it.
2. Module 5's owner creates `Post` and module 6's owner completes `Offer`; agree before starting.
3. Do you want the 300-line rule to also cover methods per class? Right now it is lines only.

## How to review this branch

Read in this order: this report → `docs/MAP.md` (check three diagrams against the files) → `git log cleanup-round-1..HEAD --oneline` for round 2, or `git log pre-cleanup..HEAD` for everything (one commit per step) → `scripts/new-methods.sh pre-cleanup` (the 72 methods that are new; almost all are the moved dialog methods). Then merge, tell the team to pull, and start on `docs/FLOWS.md`.

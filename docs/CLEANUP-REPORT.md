# Cleanup report

Branch `ccr-f2e57831-6tfkkw`, 12 commits after the tag **`pre-cleanup`** (the code as it was). The rule was: **behavior-preserving**, no new features, bugs found are written down and not fixed.

**To undo everything:** `git checkout pre-cleanup`. **To undo one step:** `git revert <commit>` using the table below.

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

## Tests: 115 run, 0 failed, 0 skipped

| Test class | Tests | What it proves |
|---|---|---|
| AuthenticationServiceTest | 13 | login, wrong password message, temporary password flag, change password rules, forgot password |
| RegistrationApplyTest | 14 | the details form, the documents per role, expiry and file rules, what `submit` saves, nothing saved when a rule fails |
| RegistrationDecisionTest | 14 | approve (account, log, upgrade, rollback) and reject, only once, admin only, the review queue |
| DocumentReviewTest | 7 | verify, undo, the Profile document list and expiry banner |
| AccountStatusTest | 9 | suspend, reinstate, the All Accounts list, the document-expiry suspension |
| SafetyServiceTest | 10 | raise, dismiss, suspend the flagged side, admin only |
| StatsServiceTest | 7 | dashboard numbers, rescue rate, monthly report, null rates |
| DialogControllerTest | 27 | the real popups: text, one open at a time, Back, callbacks, three host screens |
| FxmlWiringTest | 5 | every handler and `@FXML` field matches its FXML |
| ArchitectureTest | 8 | layer rules and the 300-line limit |
| TestDbTest | 1 | the test database loads |

**Run them:** `mvn test` (MySQL running; reads your `db.properties` login). Takes about 90 seconds.
**How I know they can fail:** I added a deliberately bad file and `ArchitectureTest` failed on three rules, then removed it. The registration and dialog tests passed unchanged across the splits, which is what they were written for.

## Behavior that changed (only these)

1. **`SafetyService.raise` now works.** It read the removed listing table and would have thrown. Deleting the stale code forced me to read the offer through `OfferDao.findParties` and a new `findStatus`. It is covered by tests.
2. **`DevPreview` is gone**, so `ANIPLATO_PREVIEW` no longer skips the login.
3. **Deleted:** `ListingService`, `ListingDao`, `Listing`, `OfferService`, `MaintenanceService` (all on the removed `listing` table; no working screen called them). `OfferDao` lost six queries on columns that no longer exist; `Offer` keeps only its `Status` enum. They return in FLOWS.md modules 5, 6, 8 and 12.
4. **The popups:** same public methods, same texts. Two null checks that could never be true were dropped (Forgot Password and Upgrade dialogs).
5. **Uploads folder** is now read from `uploads.dir` (default `uploads`, so nothing changes unless you set it).

## Bugs and gaps found (not fixed)

| # | Where | What | Suggested |
|---|---|---|---|
| 1 | `LoginController` | The "Forgot password" link on the login screen is commented out; only Profile opens that dialog. | FLOWS.md module 13 |
| 2 | `my-listings.fxml`, `new-listing.fxml` | `MyListingsController.listingCountLabel` and `NewListingController.notesArea` have no `fx:id` in the FXML, so they would be null when used. Listed in `FxmlWiringTest`. | module 5 |
| 3 | `AccountStatusService.reinstateAccount` | Spec 6.3 says the admin reinstates **with a written reason**; the method and the dialog take none. | decide: change spec or code |
| 4 | `DocumentReviewService.verifyDocument` | Spec 6.3 says a verified renewal reinstates an account suspended for "Document expired". Not implemented. | module 4 |
| 5 | `SafetyNoticesController` | Builds its own `Alert` instead of going through `DialogController` (rule: all popups). Listed in `ArchitectureTest`. | next time it is touched |
| 6 | `util/RegistrationDraft` | A util class that imports a service record (`DocumentUpload`). Listed in `ArchitectureTest`. | move the record |
| 7 | `RegisterStep2Controller` | 342 lines, over the 300 limit. Listed in `ArchitectureTest`; it may not grow. | split next time it is touched |
| 8 | `UpgradeBusinessDonorDialogController` | The file buttons put fake file names ("mayors-permit-2026.pdf") in the label; no picker yet. | module 4 |
| 9 | `SafetyNoticeDao.insert` | Leaves out the `kind` column. Harmless for concerns (MySQL fills `CONCERN`), wrong for the overdue-report notice. | module 10 |
| 10 | Eight dialogs | Cancel Listing, Confirm Release/Receipt, Flag Safety, Accept/Withdraw Offer, Upload Renewal, Upgrade only open and close until their module is built. | modules 4 to 10 |
| 11 | `docs/FLOWS.md` (old text) | Said `kind` was a schema bug and that approve/reject were new. Corrected. | done |
| 12 | `README.md` (old text) | Listed a `donor@aniplato.test` login that is not in the seed. Corrected. | done |

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

1. Reinstate with a reason? (gap 3)
2. Module 5's owner creates `Post` and module 6's owner completes `Offer`; agree before starting.
3. Do you want the 300-line rule to also cover methods per class? Right now it is lines only.
4. Keep `ArchitectureTest`'s known-exception lists, or fix those four items first?

## How to review this branch

Read in this order: this report → `docs/MAP.md` (check three chains against the files) → `git log pre-cleanup..HEAD` (one commit per step) → `scripts/new-methods.sh pre-cleanup` (the 72 methods that are new; almost all are the moved dialog methods). Then merge, tell the team to pull, and start on `docs/FLOWS.md`.

# ANIPLATO map

What is in the project, why each file exists, and which feature uses it. **Single source for "what exists"**; [FLOWS.md](FLOWS.md) says how to build what is missing. `MapCoverageTest` fails the build if a file or a public method is missing here, or if this page names a file that is gone.

**Legend:** ✅ built and tested · 🟡 partly built · ⬜ not built (screens exist as placeholders) · 🔧 infrastructure shared by every feature.
**Reading a chain:** `Screen → Controller.handler → Service.method → Dao.method → table`. ⚠ = the service can throw `RuleViolationException`, which the screen shows as a message.
**Diagram colours:** blue = screen, grey = controller or util, green = service, orange = DAO, purple = table; a dashed grey box = planned, not built.

## 1. Features

One feature is one thing a user can do. Planned ones are named too, so the split is clear before they are built.

| ID | Feature | A user can… | Status | Spec | FLOWS module |
|---|---|---|---|---|---|
| **AUTH** | Sign in and passwords | log in and out, replace a temporary password, change a password, reset a forgotten one | ✅ | 6.2 Stage 1, R1.7 | 1 |
| **REGISTER** | Apply for an account | enter details, upload documents, submit | ✅ | 6.2 Stage 1 | 2 |
| **REVIEW** | Admin reviews applications | see the queue, verify each document, approve or reject | ✅ | 6.2 Stage 1, R1.4-R1.6 | 3 |
| **ACCOUNTS** | Admin manages accounts | list accounts, suspend, reinstate | ✅ | 6.3 | 4 |
| **PROFILE** | Organization profile | see own details and documents, get expiry warnings; *edit, renew, upgrade: not built* | 🟡 | 6.3 | 4 |
| **SAFETY** | Safety notices | admin resolves notices, a concern can be raised in the service; *raising from a screen: not built* | 🟡 | 6.3, R7.1, R7.2 | 10 |
| **STATS** | Admin numbers | dashboard cards, monthly report | ✅ | Section 3, A9 | 11 |
| **MAINT** | Automatic checks | expired documents suspend the account on any screen load; *five other checks: not built* | 🟡 | 6.6 | 12 |
| **LISTING** | Post a listing (Route 1) | post surplus food, see own listings; *screens not wired yet* | 🟡 | 6.2 Stage 2A | 5 |
| **REQUEST** | Post a request (Route 2) | a food group asks for food; *screens not wired yet* | 🟡 | 6.2 Stage 2B | 9 |
| **OFFER** | Browse and send offers | browse, send, withdraw offers (both routes) | ⬜ | 6.2 Stage 3 | 6 |
| **ACCEPT** | Accept or decline | the poster picks an offer; contacts are revealed | ⬜ | 6.2 Stage 4 | 7 |
| **HANDOFF** | Handoff and cancel | confirm release, receipt, the handoff record, cancel a post | ⬜ | 6.2 Stages 5-6 | 8 |
| **REPORT** | Documentation report | the food group reports after receipt; overdue flag | ⬜ | 6.2 Stage 5 | 10 |
| **HISTORY** | Dashboards and History | donor and food group dashboards, History | ⬜ | Section 3 | 11 |
| **DEMO** | Demo data and polish | a full demo story | ⬜ | Section 5 | 13 |
| **SHELL** | The app frame | sidebar, header, navigation, logout, assistant panel | 🔧 | A5 | none |
| **POPUPS** | Dialogs | every popup, through one door | 🔧 | 7.4 | none |
| **PLATFORM** | Foundations | config, database, session, email, files, passwords, settings, exceptions | 🔧 | 7.1-7.5 | none |
| **TESTS** | Safety nets | tests, architecture guards, scripts | 🔧 | none | none |

## 2. The layers

```mermaid
flowchart LR
  classDef layer fill:#eef2ff,stroke:#4f46e5
  classDef store fill:#f3e8ff,stroke:#8a4fd1
  V[View<br/>FXML + CSS]:::layer --> C[Controller<br/>read input, show result]:::layer
  C --> S[Service<br/>every rule, A1]:::layer
  S --> D[DAO<br/>SQL only]:::layer
  D --> DB[(MySQL)]:::store
  S -. one transaction per action, A2 .-> T[Database.inTransaction]:::layer
  S -. every status change, A3 .-> L[LogDao<br/>status_change]:::layer
  M[Model<br/>entities and enums]:::layer -.-> C
  M -.-> S
  M -.-> D
```

| Layer | Folder | Holds |
|---|---|---|
| View | `view/` | one FXML per screen and one per popup; `aniplato.css` |
| Controller | `controller/` | one class per screen, `shared/dialog/` one per popup |
| Service | `service/` | the rules; the only layer that changes data |
| DAO | `dao/` | SQL only, one per table |
| Model | `model/` | entities, enums, small derived helpers |
| Util | `util/` | config, database, session, navigation, settings |

**Who owns which rule**

| Service | Owns |
|---|---|
| AuthenticationService | login, password rules, forgot password |
| RegistrationService | applying, and the admin's decision |
| DocumentReviewService | verifying documents, the Profile document lists and warnings |
| AccountStatusService | suspend, reinstate, expiry suspension, account list |
| SafetyService | safety notices |
| StatsService | every admin number (derived, never stored, A9) |
| MaintenanceService | the 6.6 checks, in order; calls other services only (A4) |
| EmailService | the four emails and `email_log` |
| FileStorageService | uploads (PDF, JPG, PNG, 5 MB) under `uploads.dir` |

**The popups.** A screen talks only to `DialogController`. It opens one of 16 cards and closes the others. To add a dialog: a card FXML in `view/shared/dialog/`, a `DialogCard` subclass, one `fx:include` in `dialog.fxml`, one `showXxxMode` in `DialogController`. `DialogControllerTest` and `FxmlWiringTest` fail if the wiring is wrong.

## 3. Something broke: which file?

| Symptom | Start at |
|---|---|
| A screen will not open or a button does nothing | its FXML and controller; `FxmlWiringTest` names the mismatch |
| A rule message is wrong or missing | the service method for that action (section 5) |
| The right rule, the wrong data on screen | the DAO method it calls |
| A popup shows wrong text or does not close | its file in `controller/shared/dialog/` and `view/shared/dialog/` |
| An email did not arrive | `service/EmailService.java`, then the `email_log` table (the error is stored) |
| "File is not in the uploads folder" | `uploads.dir` in `db.properties`, `service/FileStorageService.java` |
| An account was suspended by itself | `service/MaintenanceService.java`, `service/AccountStatusService.java` (6.6, reason "Document expired") |
| Login or password trouble | `service/AuthenticationService.java`, `dao/AccountDao.java` |
| Numbers on the admin pages look wrong | `service/StatsService.java`, then `dao/PostDao.java`, `OfferDao.java`, `ReportDao.java` |
| A test cannot connect | `src/test/resources/db.properties`, `support/TestDb.java`, your `db.properties` password |

## 4. Reference A: every file

Paths are relative to `src/main/java/com/aniplato/` unless the heading says otherwise. "Features" uses the IDs from section 1.

### Application and utilities

| File | What it does | Features |
|---|---|---|
| `aniplato/Main.java` | JavaFX entry point: builds the window, starts `Navigator` at the login screen | SHELL |
| `util/AppConfig.java` | reads `db.properties`; a `-Daniplato.<key>` system property overrides it (tests use this) | PLATFORM |
| `util/Database.java` | the only class that opens connections; runs transactions (`inTransaction`) | PLATFORM |
| `util/Session.java` | who is logged in; `requireAdmin` for admin-only services | PLATFORM, AUTH |
| `util/Navigator.java` | the only class that knows view paths: switches screens, sidebar and before-load hooks | SHELL |
| `util/Settings.java` | every setting value (spec 6.7): grace hours, report days, warning days, photos, upload MB, password length, phone digits | PLATFORM |
| `util/PasswordHasher.java` | BCrypt hash and check | AUTH, REVIEW |
| `util/SvgIcon.java` | loads the bundled SVG icons into buttons and badges | SHELL, POPUPS |
| `util/AdminSelection.java` | carries the registration the admin opened from a list to the Review screen | REVIEW |
| `util/RegistrationDraft.java` | holds the registration form between Register, Step 2 and Submitted | REGISTER |
| `exception/RuleViolationException.java` | a broken business rule; its message is shown to the user (A8) | PLATFORM |
| `exception/DataAccessException.java` | wraps an `SQLException`: a bug or an outage, not a rule | PLATFORM |

### Model

| File | What it does | Features |
|---|---|---|
| `model/Account.java` | a login; its role and status; the base of Admin, Donor and FoodGroup | AUTH, ACCOUNTS, PROFILE |
| `model/Admin.java` | the platform admin (no registration) and its profile rows | AUTH, PROFILE |
| `model/Donor.java` | a Business or Casual donor and its profile rows | PROFILE |
| `model/FoodGroup.java` | a food group and its profile rows | PROFILE |
| `model/Registration.java` | an organization's details from sign-up onward | REGISTER, REVIEW |
| `model/DocumentUpload.java` | one file chosen on the registration form with its expiry choice | REGISTER |
| `model/Document.java` | one uploaded file; a renewal is a new row; expiry helpers | REGISTER, REVIEW, PROFILE |
| `model/City.java` | a Metro Manila city; prints as its name for combo boxes | REGISTER |
| `model/SafetyNotice.java` | a safety concern on an offer, with its kind and status | SAFETY |
| `model/Post.java` | a listing or a request with its statuses, deadline and photos | LISTING, REQUEST |
| `model/FoodDetail.java` | what a donor gives: category, storage, allergens, times; the enums for them | LISTING, REQUEST |
| `model/UseDetail.java` | what a food group needs or will do: event, people, place, needed-by | LISTING, REQUEST |
| `model/Offer.java` | the offer status enum only; the full model comes with OFFER | SAFETY, STATS |

### DAOs (SQL only, one per table)

| File | What it does | Features |
|---|---|---|
| `dao/AccountDao.java` | the `account` table: find, insert, suspend, reinstate, passwords | AUTH, REVIEW, ACCOUNTS |
| `dao/RegistrationDao.java` | the `registration` table: insert, queue, decide, counts | REGISTER, REVIEW, STATS |
| `dao/DocumentDao.java` | the `document` table: insert, review, expiry queries | REGISTER, REVIEW, PROFILE, ACCOUNTS, MAINT |
| `dao/CityDao.java` | the `city` table: the registration city list | REGISTER |
| `dao/SafetyNoticeDao.java` | the `safety_notice` table: insert, resolve, admin rows | SAFETY |
| `dao/OfferDao.java` | the `offer` table: the two parties, status, report counts (rest comes with OFFER) | SAFETY, STATS |
| `dao/PostDao.java` | the `post` table: insert, the open boards, a poster's own posts, and the dashboard and report totals | LISTING, REQUEST, STATS |
| `dao/DetailDao.java` | the `food_detail` and `use_detail` tables: insert, and read them out of a joined row | LISTING, REQUEST |
| `dao/PhotoDao.java` | the `photo` table: add a photo to a post (offers and reports reuse it), list the paths | LISTING, REQUEST |
| `dao/ReportDao.java` | the `report` table: people served, report deadlines | STATS |
| `dao/LogDao.java` | `status_change` (every status change, A3) and `email_log` | PLATFORM |

### Services

| File | What it does | Features |
|---|---|---|
| `service/AuthenticationService.java` | login, logout, change and reset password, temporary password | AUTH |
| `service/RegistrationService.java` | check the form, submit, approve, reject, the review queue | REGISTER, REVIEW |
| `service/DocumentReviewService.java` | verify or undo a document, own documents, expiry warnings | REVIEW, PROFILE |
| `service/AccountStatusService.java` | suspend, reinstate, expiry suspension, account list | ACCOUNTS, MAINT, SAFETY |
| `service/SafetyService.java` | raise a concern, list, dismiss, suspend the flagged account | SAFETY |
| `service/StatsService.java` | the dashboard cards and the monthly report | STATS |
| `service/MaintenanceService.java` | runs the 6.6 checks in order, calling other services only | MAINT |
| `service/PostService.java` | create listings and requests, the two boards, a user's own posts | LISTING, REQUEST |
| `service/ReportService.java` | empty stub from the scaffold: the REPORT feature fills it (save the report, work out its deadline) | REPORT |
| `service/EmailService.java` | fills the HTML emails, sends, logs every attempt | AUTH, REGISTER, REVIEW |
| `service/FileStorageService.java` | validates and stores uploads; saves 1 to 5 photos at once; removes files; finds a stored file | REGISTER, PROFILE, REVIEW, LISTING |

### Controllers: screens

| File | What it does | Features |
|---|---|---|
| `controller/LoginController.java` | login form, temporary-password dialog, Forgot password link | AUTH |
| `controller/RegisterController.java` | registration step 1: role, details, city | REGISTER |
| `controller/RegisterStep2Controller.java` | registration step 2: documents, consent, submit | REGISTER |
| `controller/UploadSlot.java` | one document row: file picker, file pill, expiry date | REGISTER, PROFILE |
| `controller/RegisterSubmittedController.java` | the "we received it" screen; clears the draft | REGISTER |
| `controller/ProfileController.java` | own details, documents, expiry banner, password buttons | PROFILE, AUTH |
| `controller/ShellController.java` | header, sidebar, badges, assistant panel, logout, runs the 6.6 checks | SHELL, MAINT |
| `controller/admin/AdminDashboardController.java` | admin cards and the pending list | STATS, REVIEW |
| `controller/admin/PendingRegistrationsController.java` | the review queue | REVIEW |
| `controller/admin/ReviewRegistrationController.java` | one application: documents, verify, approve, reject | REVIEW |
| `controller/admin/AccountsController.java` | all accounts, suspend and reinstate | ACCOUNTS |
| `controller/admin/SafetyNoticesController.java` | the notice list and resolving them | SAFETY |
| `controller/admin/ReportsController.java` | the monthly report | STATS |
| `controller/admin/AdminTable.java` | shared building blocks for the admin tables (cells, pills, dates) | REVIEW, ACCOUNTS, SAFETY |
| `controller/admin/Pager.java` | page arithmetic for the admin lists | REVIEW, ACCOUNTS |
| `controller/donor/DonorDashboardController.java` | donor dashboard (placeholder) | HISTORY |
| `controller/donor/NewListingController.java` | new listing form (placeholder, photo picker works) | LISTING |
| `controller/donor/BrowseRequestsController.java` | donor browses requests (placeholder) | OFFER |
| `controller/donor/MakeOfferController.java` | donor's offer form (placeholder, photo picker works) | OFFER |
| `controller/donor/IncomingOffersController.java` | offers on a donor's listing (placeholder) | ACCEPT |
| `controller/foodgroup/FoodGroupDashboardController.java` | food group dashboard (placeholder) | HISTORY |
| `controller/foodgroup/BrowseListingsController.java` | food group browses listings (placeholder) | OFFER |
| `controller/foodgroup/MakeOfferController.java` | food group's offer form (placeholder) | OFFER |
| `controller/foodgroup/MyOffersController.java` | food group's offers (placeholder) | OFFER |
| `controller/foodgroup/FoodGroupIncomingOffersController.java` | offers on a food group's request (placeholder) | ACCEPT |
| `controller/foodgroup/NewRequestController.java` | new request form (placeholder) | REQUEST |
| `controller/foodgroup/MyRequestsController.java` | food group's requests (placeholder) | REQUEST |
| `controller/foodgroup/SubmitReportController.java` | the report form (placeholder) | REPORT |
| `controller/shared/MyListingsController.java` | donor's listings and the cancel dialog (placeholder) | LISTING, HANDOFF |
| `controller/shared/MyOffersController.java` | offers screen whose copy follows the signed-in role (placeholder) | OFFER |
| `controller/shared/HandoffRecordController.java` | one handoff record with its dialogs (placeholder) | HANDOFF |
| `controller/shared/HistoryController.java` | History with sample rows (placeholder) | HISTORY |

### Controllers: popups

| File | What it does | Features |
|---|---|---|
| `controller/shared/DialogController.java` | the one door to every popup: opens one card, closes the rest, `showError` | POPUPS |
| `controller/shared/dialog/DialogCard.java` | what every dialog shares: its card, open, close, confirm | POPUPS |
| `controller/shared/dialog/DialogHost.java` | what a dialog may ask of its overlay | POPUPS |
| `controller/shared/dialog/RejectDialogController.java` | reject an application with a reason | REVIEW |
| `controller/shared/dialog/ApproveDialogController.java` | approve an application | REVIEW |
| `controller/shared/dialog/SuspendAccountDialogController.java` | suspend with a reason | ACCOUNTS |
| `controller/shared/dialog/ReinstateAccountDialogController.java` | reinstate | ACCOUNTS |
| `controller/shared/dialog/ResolveSafetyDialogController.java` | dismiss or suspend from a safety notice | SAFETY |
| `controller/shared/dialog/NoticeResolvedDialogController.java` | shows how a closed notice was resolved | SAFETY |
| `controller/shared/dialog/FlagSafetyDialogController.java` | flag a concern (opens and closes only) | SAFETY |
| `controller/shared/dialog/SetNewPasswordDialogController.java` | replace a temporary password or change a password | AUTH |
| `controller/shared/dialog/ForgotPasswordDialogController.java` | email a temporary password | AUTH |
| `controller/shared/dialog/UploadRenewalDialogController.java` | upload a renewal (opens and closes only) | PROFILE |
| `controller/shared/dialog/UpgradeBusinessDonorDialogController.java` | upgrade to Business donor (opens and closes only) | PROFILE |
| `controller/shared/dialog/CancelListingDialogController.java` | cancel a post (opens and closes only) | HANDOFF |
| `controller/shared/dialog/ConfirmReleaseDialogController.java` | confirm release (opens and closes only) | HANDOFF |
| `controller/shared/dialog/ConfirmReceiptDialogController.java` | confirm receipt (opens and closes only) | HANDOFF |
| `controller/shared/dialog/AcceptOfferDialogController.java` | accept an offer (opens and closes only) | ACCEPT |
| `controller/shared/dialog/WithdrawOfferDialogController.java` | withdraw an offer (opens and closes only) | OFFER |

### Views (`view/`)

| File | What it does | Features |
|---|---|---|
| `view/aniplato.css` | the one stylesheet for every screen and popup | SHELL, POPUPS |
| `view/login.fxml` | login screen | AUTH |
| `view/register.fxml` | registration step 1 | REGISTER |
| `view/register-step2.fxml` | registration step 2 | REGISTER |
| `view/register-submitted.fxml` | registration received | REGISTER |
| `view/profile.fxml` | organization profile | PROFILE |
| `view/shared/shell.fxml` | header, sidebar and content area around every logged-in screen | SHELL |
| `view/shared/dialog.fxml` | the popup overlay that includes the 16 cards | POPUPS |
| `view/shared/handoff-record.fxml` | handoff record | HANDOFF |
| `view/shared/history.fxml` | History | HISTORY |
| `view/shared/my-offers.fxml` | offers screen shared by both donor roles | OFFER |
| `view/admin/dashboard.fxml` | admin dashboard | STATS, REVIEW |
| `view/admin/pending-registrations.fxml` | the review queue | REVIEW |
| `view/admin/review-registration.fxml` | one application | REVIEW |
| `view/admin/accounts.fxml` | all accounts | ACCOUNTS |
| `view/admin/safety-notices.fxml` | safety notices | SAFETY |
| `view/admin/reports.fxml` | monthly report | STATS |
| `view/donor/dashboard.fxml` | donor dashboard | HISTORY |
| `view/donor/new-listing.fxml` | new listing | LISTING |
| `view/donor/my-listings.fxml` | my listings | LISTING |
| `view/donor/browse-request.fxml` | browse requests | OFFER |
| `view/donor/make-offer.fxml` | donor offer form | OFFER |
| `view/donor/incoming-offers.fxml` | offers on my listing | ACCEPT |
| `view/foodgroup/dashboard.fxml` | food group dashboard | HISTORY |
| `view/foodgroup/browse-listings.fxml` | browse listings | OFFER |
| `view/foodgroup/make-offer.fxml` | food group offer form | OFFER |
| `view/foodgroup/my-offers.fxml` | my offers | OFFER |
| `view/foodgroup/incoming-offers.fxml` | offers on my request | ACCEPT |
| `view/foodgroup/new-request.fxml` | new request | REQUEST |
| `view/foodgroup/my-requests.fxml` | my requests | REQUEST |
| `view/foodgroup/submit-report.fxml` | submit report | REPORT |
| `view/shared/dialog/reject.fxml` | Reject card | REVIEW |
| `view/shared/dialog/approve.fxml` | Approve card | REVIEW |
| `view/shared/dialog/suspend-account.fxml` | Suspend card | ACCOUNTS |
| `view/shared/dialog/reinstate-account.fxml` | Reinstate card | ACCOUNTS |
| `view/shared/dialog/resolve-safety.fxml` | Resolve notice card | SAFETY |
| `view/shared/dialog/notice-resolved.fxml` | Notice resolved card | SAFETY |
| `view/shared/dialog/flag-safety.fxml` | Flag concern card | SAFETY |
| `view/shared/dialog/set-new-password.fxml` | Set new password card | AUTH |
| `view/shared/dialog/forgot-password.fxml` | Forgot password card | AUTH |
| `view/shared/dialog/upload-renewal.fxml` | Upload renewal card | PROFILE |
| `view/shared/dialog/upgrade-business-donor.fxml` | Upgrade card | PROFILE |
| `view/shared/dialog/cancel-listing.fxml` | Cancel post card | HANDOFF |
| `view/shared/dialog/confirm-release.fxml` | Confirm release card | HANDOFF |
| `view/shared/dialog/confirm-receipt.fxml` | Confirm receipt card | HANDOFF |
| `view/shared/dialog/accept-offer.fxml` | Accept offer card | ACCEPT |
| `view/shared/dialog/withdraw-offer.fxml` | Withdraw offer card | OFFER |

### Resources, database, scripts and build

| File | What it does | Features |
|---|---|---|
| `resources/db.properties.example` | the config to copy to `db.properties` (database, SMTP, `uploads.dir`) | PLATFORM |
| `email/received.html`, `email/approved.html`, `email/not-approved.html`, `email/forgot-password.html` | the four HTML email templates | REGISTER, REVIEW, AUTH |
| `assets/AcceptedOffers.svg`, `assets/AllAccountsIcon.svg`, `assets/BrowseListings.svg`, `assets/BrowseRequest.svg`, `assets/CIrcle_Check_Button.svg`, `assets/Check_Button.svg`, `assets/Circle_Reject_Button.svg`, `assets/Dashboard Icon.svg`, `assets/HistoryIcon.svg`, `assets/KgRescued.svg`, `assets/MyListingsIcon.svg`, `assets/MyOffers.svg`, `assets/MyRequests.svg`, `assets/NewListingIcon.svg`, `assets/NewRequest.svg`, `assets/OpenListings.svg`, `assets/PendingAccountsIcon.svg`, `assets/ProfileIcon.svg`, `assets/Reject_Button.svg`, `assets/ReportsIcon.svg`, `assets/SafetyNotices.svg` | sidebar, card and badge icons | SHELL, POPUPS |
| `assets/aniplato-logo-circle 1.png`, `assets/login_hero.png` | the logo (window, emails) and the login picture | SHELL, AUTH |
| `sql/schema.sql` | the only copy of the schema; drops and recreates `aniplato` | PLATFORM |
| `sql/seed.sql` | demo data: admin, three organizations, one pending application, a listing and a request | DEMO |
| `sql/seed-admin-demo.sql` | extra demo rows for the admin screens; run once after the seed | DEMO |
| `setup-db.bat`, `setup-db.ps1` | create the database and load the demo data | PLATFORM |
| `scripts/new-methods.sh` | lists the methods a branch adds, for review | TESTS |
| `pom.xml` | Maven build: JavaFX, MySQL, JUnit, copies FXML and CSS to the output | PLATFORM |

### Tests (`src/test/java/com/aniplato/`)

| File | What it proves | Features |
|---|---|---|
| `aniplato/ArchitectureTest.java` | layer rules and the 300-line limit | TESTS |
| `aniplato/MapCoverageTest.java` | this page lists every file and public method | TESTS |
| `service/AuthenticationServiceTest.java` | login, password rules, forgot password | AUTH |
| `service/RegistrationApplyTest.java` | the form, documents, what `submit` saves | REGISTER |
| `service/RegistrationDecisionTest.java` | approve, reject, the queue, rollback | REVIEW |
| `service/DocumentReviewTest.java` | verify, undo, own documents, warnings | REVIEW, PROFILE |
| `service/AccountStatusTest.java` | suspend, reinstate, account list, expiry suspension | ACCOUNTS |
| `service/SafetyServiceTest.java` | raise, dismiss, suspend the flagged side | SAFETY |
| `service/StatsServiceTest.java` | dashboard numbers and the monthly report | STATS |
| `service/PostServiceTest.java` | posting rules, the boards, hidden suspended posters, my posts | LISTING, REQUEST |
| `service/MaintenanceServiceTest.java` | the expiry check runs once and is quiet when nothing expired | MAINT |
| `dao/SafetyNoticeDaoTest.java` | the notice kind is written and read | SAFETY |
| `model/AccountTest.java` | a Suspended account may not post or offer | PROFILE |
| `controller/DialogControllerTest.java` | the real popups: text, one open at a time, Back, callbacks | POPUPS |
| `controller/LoginScreenTest.java` | the Forgot password link opens the dialog with the typed email | AUTH |
| `controller/ShellMaintenanceTest.java` | the 6.6 checks run before each screen loads | MAINT, SHELL |
| `controller/UploadSlotTest.java` | one upload row shows, restores and reports its file | REGISTER |
| `view/FxmlWiringTest.java` | every handler and `@FXML` field matches its FXML | TESTS |
| `support/DbTest.java` | base class: fresh seeded test database, nobody logged in | TESTS |
| `support/TestDb.java` | builds `aniplato_test` from the schema and seed; refuses any other database | TESTS |
| `support/TestDbTest.java` | the test database loads | TESTS |
| `support/Fixtures.java` | SQL helpers for rows the seed lacks (offers, notices) and sample photo files | TESTS |
| `support/Fx.java` | runs test code on the JavaFX thread | TESTS |
| `resources/db.properties` | test config under src/test/resources: no SMTP keys, so tests never send email | TESTS |

### Docs and project files

| File | What it does | Features |
|---|---|---|
| `docs/Project Spec.md` | the PRD: flows, rules A1-A9, data model; the source of truth | all |
| `docs/MAP.md` | this page | all |
| `docs/FLOWS.md` | build order, conventions C1-C9, one blueprint per unbuilt module | all |
| `docs/CLEANUP-REPORT.md` | what the cleanup changed, results, gaps | TESTS |
| `docs/ARCHITECTURE.md` | the ER diagram | PLATFORM |
| `docs/FXML-GUIDE.md` | how to build a screen in Scene Builder so the Java side can wire it | SHELL |
| `docs/notes/model-report.md` | notes on the model classes | PLATFORM |
| `AGENTS.md` | rules every AI assistant and developer follows | all |
| `CLAUDE.md` | the coding rules summary | all |
| `README.md` | setup and layout | all |
| `LICENSE` | the licence | all |
| `.gitignore`, `.gitattributes` | what git skips; `*.sh` keep Unix line endings | PLATFORM |
| `uploads/.gitkeep` | keeps the git-ignored uploads folder in the repo | PLATFORM |

## 5. Reference B: every feature, its files and methods

Methods are listed as `Class.method`. "Called by" is read from the code; "Test" is the test class that covers it (DAO methods are covered through the service tests).

### AUTH: sign in and passwords ✅

Files: `service/AuthenticationService.java`, `controller/LoginController.java`, `controller/shared/dialog/SetNewPasswordDialogController.java`, `controller/shared/dialog/ForgotPasswordDialogController.java`, `controller/ProfileController.java`, `view/login.fxml`.

| Method | What it does | Called by | Test |
|---|---|---|---|
| `AuthenticationService.login` | checks email and password, starts the session; a Suspended account can log in | LoginController | AuthenticationServiceTest |
| `AuthenticationService.logout` | clears the session | ShellController | AuthenticationServiceTest |
| `AuthenticationService.mustChangePassword` | true while the user has a temporary password | **nobody**: LoginController reads `Account.mustChangePassword` | AuthenticationServiceTest |
| `AuthenticationService.changePassword` | new password: 8+ characters, different, confirmed | SetNewPasswordDialogController | AuthenticationServiceTest |
| `AuthenticationService.requestPasswordReset` | emails a temporary password, replaces the password only after the email is sent | ForgotPasswordDialogController | AuthenticationServiceTest |
| `AuthenticationService.generateTemporaryPassword` | 12 random characters without look-alikes | RegistrationService (approve) | AuthenticationServiceTest |
| `AccountDao.findByEmail` | the account (with its registration) for an email | AuthenticationService, RegistrationService | AuthenticationServiceTest |
| `AccountDao.findPasswordHash` | the stored hash, read only when needed | AuthenticationService | AuthenticationServiceTest |
| `AccountDao.updatePassword` | saves a new hash and clears the temporary flag | AuthenticationService | AuthenticationServiceTest |
| `AccountDao.setTemporaryPassword` | stores a temporary hash and sets the flag | AuthenticationService | AuthenticationServiceTest |
| `PasswordHasher.hash` | BCrypt hash | AuthenticationService, RegistrationService | AuthenticationServiceTest |
| `PasswordHasher.matches` | BCrypt check | AuthenticationService | AuthenticationServiceTest |
| `EmailService.sendPasswordReset` | the reset email | AuthenticationService | AuthenticationServiceTest (send fails, is logged) |

**Flow**

```mermaid
flowchart LR
  classDef screen fill:#e8f1ff,stroke:#3b6fd4,color:#111827
  classDef ctrl fill:#f1f5f9,stroke:#64748b,color:#111827
  classDef svc fill:#e8f7ee,stroke:#2f9e5a,color:#111827
  classDef dao fill:#fff4e0,stroke:#d98a00,color:#111827
  classDef tbl fill:#f3e8ff,stroke:#8a4fd1,color:#111827
  LG([login.fxml]):::screen --> LC[LoginController<br/>handleLogin]:::ctrl
  LC --> LO{{AuthenticationService<br/>login}}:::svc
  LO -. rule fails, message shown .-> LC
  LO --> FE[AccountDao<br/>findByEmail<br/>findPasswordHash]:::dao --> TA[(account)]:::tbl
  LO --> SS[Session.start]:::ctrl
  LC -- temporary password --> NP[SetNewPasswordDialogController]:::ctrl --> CP{{AuthenticationService<br/>changePassword}}:::svc --> UP[AccountDao<br/>updatePassword]:::dao --> TA
  PF([profile.fxml<br/>or the login link]):::screen --> FP[ForgotPasswordDialogController]:::ctrl --> RP{{AuthenticationService<br/>requestPasswordReset}}:::svc
  RP --> EM[EmailService<br/>sendPasswordReset]:::svc --> EL[(email_log)]:::tbl
  RP --> TP[AccountDao<br/>setTemporaryPassword]:::dao --> TA
```

### REGISTER: apply for an account ✅

Files: `service/RegistrationService.java`, `controller/RegisterController.java`, `controller/RegisterStep2Controller.java`, `controller/UploadSlot.java`, `controller/RegisterSubmittedController.java`, `util/RegistrationDraft.java`, `model/DocumentUpload.java`, `view/register.fxml`, `view/register-step2.fxml`, `view/register-submitted.fxml`, `email/received.html`.

| Method | What it does | Called by | Test |
|---|---|---|---|
| `RegistrationService.cities` | the city list for the form | RegisterController | RegistrationApplyTest |
| `RegistrationService.checkDetails` | every detail present, role chosen, 11-digit phone, valid email, email not already an account | RegisterController | RegistrationApplyTest |
| `RegistrationService.submit` | checks the documents, saves registration, files, documents and log in one transaction, then sends the Received email | RegisterStep2Controller | RegistrationApplyTest |
| `CityDao.findAll` | all cities by name | RegistrationService | RegistrationApplyTest |
| `AccountDao.existsByEmail` | true if a login already uses the email | RegistrationService | RegistrationApplyTest |
| `RegistrationDao.insert` | saves a Pending registration | RegistrationService | RegistrationApplyTest |
| `DocumentDao.insert` | saves an uploaded file awaiting review | RegistrationService | RegistrationApplyTest |
| `FileStorageService.validate` | PDF, JPG or PNG, at most 5 MB | RegistrationService, UploadSlot | RegistrationApplyTest |
| `FileStorageService.save` | copies the file to `uploads.dir` under a random name | RegistrationService | RegistrationApplyTest |
| `FileStorageService.resolve` | the stored file for a saved path | ProfileController, ReviewRegistrationController | RegistrationApplyTest |
| `EmailService.sendReceived` | the "we received your registration" email | RegistrationService | RegistrationApplyTest |
| `RegistrationDraft.get` | the form kept between steps | RegisterController, RegisterStep2Controller, RegisterSubmittedController | none (UI state) |
| `RegistrationDraft.set` | keeps the step 1 form | RegisterController | none |
| `RegistrationDraft.uploads` | the files chosen on step 2, kept while going Back | RegisterStep2Controller | UploadSlotTest |
| `RegistrationDraft.setUploads` | remembers them on Back | RegisterStep2Controller | none |
| `RegistrationDraft.clear` | forgets the draft once submitted | RegisterSubmittedController | none |

**Flow**

```mermaid
flowchart LR
  classDef screen fill:#e8f1ff,stroke:#3b6fd4,color:#111827
  classDef ctrl fill:#f1f5f9,stroke:#64748b,color:#111827
  classDef svc fill:#e8f7ee,stroke:#2f9e5a,color:#111827
  classDef dao fill:#fff4e0,stroke:#d98a00,color:#111827
  classDef tbl fill:#f3e8ff,stroke:#8a4fd1,color:#111827
  S1([register.fxml]):::screen --> RC[RegisterController]:::ctrl --> CD{{RegistrationService<br/>cities and checkDetails}}:::svc
  CD --> CI[CityDao<br/>findAll]:::dao --> TC[(city)]:::tbl
  CD --> AE[AccountDao<br/>existsByEmail]:::dao --> TA[(account)]:::tbl
  S2([register-step2.fxml]):::screen --> R2[RegisterStep2Controller<br/>with UploadSlot]:::ctrl --> SB{{RegistrationService<br/>submit}}:::svc
  SB -. rule fails, message in place .-> R2
  subgraph TX[one transaction A2]
    FS[FileStorageService<br/>save]:::svc
    RD[RegistrationDao<br/>insert]:::dao
    DD[DocumentDao<br/>insert]:::dao
    LG[LogDao<br/>logStatusChange]:::dao
  end
  SB --> FS
  SB --> RD
  SB --> DD
  SB --> LG
  FS --> UP[(uploads folder)]:::tbl
  RD --> TR[(registration)]:::tbl
  DD --> TD[(document)]:::tbl
  LG --> TL[(status_change)]:::tbl
  SB --> EM[EmailService<br/>sendReceived]:::svc --> EL[(email_log)]:::tbl
```

### REVIEW: the admin reviews applications ✅

Files: `service/RegistrationService.java`, `service/DocumentReviewService.java`, `controller/admin/PendingRegistrationsController.java`, `controller/admin/ReviewRegistrationController.java`, `controller/admin/AdminTable.java`, `controller/admin/Pager.java`, `controller/shared/dialog/ApproveDialogController.java`, `controller/shared/dialog/RejectDialogController.java`, `util/AdminSelection.java`, `view/admin/pending-registrations.fxml`, `view/admin/review-registration.fxml`, `email/approved.html`, `email/not-approved.html`.

| Method | What it does | Called by | Test |
|---|---|---|---|
| `RegistrationService.getPending` | Pending applications, oldest first, marked "registered again" | PendingRegistrationsController, AdminDashboardController | RegistrationDecisionTest |
| `RegistrationService.getForReview` | one application with all its documents | ReviewRegistrationController | RegistrationDecisionTest |
| `RegistrationService.approve` | every document verified, unexpired, unflagged; creates the account (or moves an upgrade); one transaction; then the Approved email | ApproveDialogController | RegistrationDecisionTest |
| `RegistrationService.reject` | needs a reason (500 max); stores it; no account; then the Not approved email | RejectDialogController | RegistrationDecisionTest |
| `DocumentReviewService.verifyDocument` | admin marks a document OK | ReviewRegistrationController | DocumentReviewTest |
| `DocumentReviewService.undoVerification` | takes the verification back | ReviewRegistrationController | DocumentReviewTest |
| `RegistrationDao.findByStatus` | applications in one status, oldest first | RegistrationService | RegistrationDecisionTest |
| `RegistrationDao.findById` | one application with its city | RegistrationService | RegistrationDecisionTest |
| `RegistrationDao.countEarlierRejected` | earlier rejections for the same email | RegistrationService | RegistrationDecisionTest |
| `RegistrationDao.approve` | Pending to Approved, with the reviewer | RegistrationService | RegistrationDecisionTest |
| `RegistrationDao.reject` | Pending to Rejected, with reason and reviewer | RegistrationService | RegistrationDecisionTest |
| `DocumentDao.findByRegistration` | every file of a registration, oldest first | RegistrationService, DocumentReviewService | RegistrationDecisionTest |
| `DocumentDao.findById` | one document | DocumentReviewService | DocumentReviewTest |
| `DocumentDao.markOk` | Awaiting to OK (approve step) | RegistrationService | RegistrationDecisionTest |
| `DocumentDao.markReviewed` | the admin's verdict: OK, FLAGGED with a reason, or back to awaiting | DocumentReviewService | DocumentReviewTest |
| `AccountDao.insert` | creates the login of an approved registration | RegistrationService | RegistrationDecisionTest |
| `AccountDao.moveToRegistration` | an approved upgrade: same account, new registration and role | RegistrationService | RegistrationDecisionTest |
| `EmailService.sendApproved` | the temporary login email | RegistrationService | RegistrationDecisionTest |
| `EmailService.sendNotApproved` | the rejection reasons email | RegistrationService | RegistrationDecisionTest |
| `AdminSelection.selectRegistration` | remembers the application the admin opened | PendingRegistrationsController, AdminDashboardController | none (UI state) |
| `AdminSelection.registrationId` | reads it on the Review screen | ReviewRegistrationController | none |

**Flow**

```mermaid
flowchart LR
  classDef screen fill:#e8f1ff,stroke:#3b6fd4,color:#111827
  classDef ctrl fill:#f1f5f9,stroke:#64748b,color:#111827
  classDef svc fill:#e8f7ee,stroke:#2f9e5a,color:#111827
  classDef dao fill:#fff4e0,stroke:#d98a00,color:#111827
  classDef tbl fill:#f3e8ff,stroke:#8a4fd1,color:#111827
  Q([pending-registrations.fxml]):::screen --> PC[PendingRegistrationsController]:::ctrl --> GP{{RegistrationService<br/>getPending}}:::svc --> RF[RegistrationDao<br/>findByStatus]:::dao --> TR[(registration)]:::tbl
  RV([review-registration.fxml]):::screen --> RC[ReviewRegistrationController]:::ctrl
  RC --> GR{{RegistrationService<br/>getForReview}}:::svc --> RF
  RC --> VD{{DocumentReviewService<br/>verifyDocument<br/>undoVerification}}:::svc --> MR[DocumentDao<br/>markReviewed]:::dao --> TD[(document)]:::tbl
  RC --> AD[ApproveDialogController]:::ctrl --> AP{{RegistrationService<br/>approve}}:::svc
  RC --> RJD[RejectDialogController]:::ctrl --> RJ{{RegistrationService<br/>reject}}:::svc
  AP -. rule fails, message shown .-> AD
  subgraph TX[one transaction A2 for approve]
    M1[DocumentDao<br/>markOk]:::dao
    M2[RegistrationDao<br/>approve]:::dao
    M3[AccountDao<br/>insert or moveToRegistration]:::dao
    LG[LogDao]:::dao
  end
  AP --> M1
  AP --> M2
  AP --> M3
  AP --> LG
  M1 --> TD
  M2 --> TR
  M3 --> TA[(account)]:::tbl
  LG --> TL[(status_change)]:::tbl
  AP --> EA[EmailService<br/>sendApproved]:::svc
  RJ --> M4[RegistrationDao<br/>reject]:::dao --> TR
  RJ --> EN[EmailService<br/>sendNotApproved]:::svc
```

### ACCOUNTS: the admin manages accounts ✅

Files: `service/AccountStatusService.java`, `controller/admin/AccountsController.java`, `controller/shared/dialog/SuspendAccountDialogController.java`, `controller/shared/dialog/ReinstateAccountDialogController.java`, `view/admin/accounts.fxml`.

| Method | What it does | Called by | Test |
|---|---|---|---|
| `AccountStatusService.getAccounts` | every organization with its next ending document; counts worked out from the rows | AccountsController | AccountStatusTest |
| `AccountStatusService.suspendAccount` | admin suspends an Active account with a reason | AccountsController | AccountStatusTest |
| `AccountStatusService.reinstateAccount` | admin gives a Suspended account its access back | AccountsController | AccountStatusTest |
| `AccountStatusService.suspend` | the suspension itself, inside someone else's transaction | SafetyService | SafetyServiceTest |
| `AccountDao.findAllOrganizations` | donors and food groups by name, with their registration | AccountStatusService | AccountStatusTest |
| `AccountDao.suspendIfActive` | Active to Suspended; 0 rows if it was not Active | AccountStatusService | AccountStatusTest |
| `AccountDao.reinstateIfSuspended` | Suspended to Active; 0 rows if it was not Suspended | AccountStatusService | AccountStatusTest |
| `DocumentDao.findEndingBy` | verified documents in force that end by a date | AccountStatusService, StatsService | AccountStatusTest |

**Flow**

```mermaid
flowchart LR
  classDef screen fill:#e8f1ff,stroke:#3b6fd4,color:#111827
  classDef ctrl fill:#f1f5f9,stroke:#64748b,color:#111827
  classDef svc fill:#e8f7ee,stroke:#2f9e5a,color:#111827
  classDef dao fill:#fff4e0,stroke:#d98a00,color:#111827
  classDef tbl fill:#f3e8ff,stroke:#8a4fd1,color:#111827
  A([accounts.fxml]):::screen --> AC[AccountsController]:::ctrl --> GA{{AccountStatusService<br/>getAccounts}}:::svc
  GA --> FO[AccountDao<br/>findAllOrganizations]:::dao --> TA[(account)]:::tbl
  GA --> FE[DocumentDao<br/>findEndingBy]:::dao --> TD[(document)]:::tbl
  AC --> SD[SuspendAccountDialogController]:::ctrl --> SA{{AccountStatusService<br/>suspendAccount}}:::svc
  AC --> RD[ReinstateAccountDialogController]:::ctrl --> RA{{AccountStatusService<br/>reinstateAccount}}:::svc
  SA -. rule fails, message shown .-> SD
  subgraph TX[one transaction A2]
    SI[AccountDao<br/>suspendIfActive]:::dao
    RI[AccountDao<br/>reinstateIfSuspended]:::dao
    LG[LogDao]:::dao
  end
  SA --> SI
  RA --> RI
  SA --> LG
  RA --> LG
  SI --> TA
  RI --> TA
  LG --> TL[(status_change)]:::tbl
```

### PROFILE: the organization's own profile 🟡

Files: `controller/ProfileController.java`, `view/profile.fxml`, `controller/shared/dialog/UploadRenewalDialogController.java` and `controller/shared/dialog/UpgradeBusinessDonorDialogController.java` (placeholders), `model/Admin.java`, `model/Donor.java`, `model/FoodGroup.java`.

| Method | What it does | Called by | Test |
|---|---|---|---|
| `DocumentReviewService.getMyDocuments` | the latest upload of each document type | ProfileController | DocumentReviewTest |
| `DocumentReviewService.getCurrentDocumentAlerts` | verified documents expired or ending within 30 days | ProfileController | DocumentReviewTest |
| `DocumentDao.findCurrentAlerts` | the query behind the warning banner | DocumentReviewService | DocumentReviewTest |

**Flow** (dashed = not built yet)

```mermaid
flowchart LR
  classDef screen fill:#e8f1ff,stroke:#3b6fd4,color:#111827
  classDef ctrl fill:#f1f5f9,stroke:#64748b,color:#111827
  classDef svc fill:#e8f7ee,stroke:#2f9e5a,color:#111827
  classDef dao fill:#fff4e0,stroke:#d98a00,color:#111827
  classDef tbl fill:#f3e8ff,stroke:#8a4fd1,color:#111827
  classDef planned fill:#ffffff,stroke:#94a3b8,stroke-dasharray:5 3,color:#475569
  P([profile.fxml]):::screen --> PC[ProfileController]:::ctrl
  PC --> MD{{DocumentReviewService<br/>getMyDocuments<br/>getCurrentDocumentAlerts}}:::svc --> DF[DocumentDao<br/>findByRegistration<br/>findCurrentAlerts]:::dao --> TD[(document)]:::tbl
  PC --> FS[FileStorageService<br/>resolve]:::svc --> UP[(uploads folder)]:::tbl
  PC -. password buttons .-> AU[see AUTH]:::ctrl
  PC -.-> ED[RegistrationService<br/>updateProfile<br/>submitUpgrade]:::planned
  PC -.-> RN[DocumentReviewService<br/>uploadRenewal]:::planned
```

### SAFETY: safety notices 🟡

Files: `service/SafetyService.java`, `dao/SafetyNoticeDao.java`, `model/SafetyNotice.java`, `controller/admin/SafetyNoticesController.java`, `controller/shared/dialog/ResolveSafetyDialogController.java`, `controller/shared/dialog/NoticeResolvedDialogController.java`, `controller/shared/dialog/FlagSafetyDialogController.java` (placeholder), `view/admin/safety-notices.fxml`.

| Method | What it does | Called by | Test |
|---|---|---|---|
| `SafetyService.raise` | either side of an Accepted or Completed offer flags a concern about the other | **no screen yet** | SafetyServiceTest |
| `SafetyService.getNoticeRows` | every notice with both organization names, newest first | SafetyNoticesController | SafetyServiceTest |
| `SafetyService.dismiss` | admin closes a notice with a note | SafetyNoticesController | SafetyServiceTest |
| `SafetyService.suspendFlaggedAccount` | admin suspends the other side and closes the notice; one transaction | SafetyNoticesController | SafetyServiceTest |
| `SafetyNoticeDao.insert` | saves a notice with its kind | SafetyService | SafetyNoticeDaoTest |
| `SafetyNoticeDao.findById` | one notice | SafetyService | SafetyNoticeDaoTest |
| `SafetyNoticeDao.resolve` | Open to Dismissed or Account suspended, with note and admin | SafetyService | SafetyServiceTest |
| `SafetyNoticeDao.findRows` | notices joined with the organization names | SafetyService | SafetyServiceTest |
| `SafetyNoticeDao.countOpen` | open notices, for the dashboard | StatsService | StatsServiceTest |
| `OfferDao.findParties` | who made an offer and who posted what it is on | SafetyService | SafetyServiceTest |
| `OfferDao.findStatus` | an offer's status | SafetyService | SafetyServiceTest |

**Flow** (dashed = not built yet)

```mermaid
flowchart LR
  classDef screen fill:#e8f1ff,stroke:#3b6fd4,color:#111827
  classDef ctrl fill:#f1f5f9,stroke:#64748b,color:#111827
  classDef svc fill:#e8f7ee,stroke:#2f9e5a,color:#111827
  classDef dao fill:#fff4e0,stroke:#d98a00,color:#111827
  classDef tbl fill:#f3e8ff,stroke:#8a4fd1,color:#111827
  classDef planned fill:#ffffff,stroke:#94a3b8,stroke-dasharray:5 3,color:#475569
  N([safety-notices.fxml]):::screen --> NC[SafetyNoticesController]:::ctrl --> GR{{SafetyService<br/>getNoticeRows}}:::svc --> FR[SafetyNoticeDao<br/>findRows]:::dao --> TN[(safety_notice)]:::tbl
  NC --> RS[ResolveSafetyDialogController]:::ctrl
  RS --> DI{{SafetyService<br/>dismiss}}:::svc
  RS --> SF{{SafetyService<br/>suspendFlaggedAccount}}:::svc
  DI -. rule fails, message shown .-> RS
  subgraph TX[one transaction A2]
    RE[SafetyNoticeDao<br/>resolve]:::dao
    PA[OfferDao<br/>findParties]:::dao
    SU{{AccountStatusService<br/>suspend}}:::svc
    LG[LogDao]:::dao
  end
  DI --> RE
  SF --> RE
  SF --> PA
  SF --> SU
  DI --> LG
  SF --> LG
  RE --> TN
  PA --> TO[(offer)]:::tbl
  SU --> TA[(account)]:::tbl
  LG --> TL[(status_change)]:::tbl
  NC --> NR[NoticeResolvedDialogController<br/>shows the result]:::ctrl
  FD([Flag concern dialog<br/>not wired yet]):::planned -.-> RA{{SafetyService<br/>raise}}:::svc
  RA --> OS[OfferDao<br/>findStatus<br/>findParties]:::dao --> TO
  RA --> IN[SafetyNoticeDao<br/>insert]:::dao --> TN
```

### STATS: admin numbers ✅

Files: `service/StatsService.java`, `controller/admin/AdminDashboardController.java`, `controller/admin/ReportsController.java`, `view/admin/dashboard.fxml`, `view/admin/reports.fxml`, the badges in `controller/ShellController.java`.

| Method | What it does | Called by | Test |
|---|---|---|---|
| `StatsService.getAdminSummary` | the six dashboard cards for the current month | AdminDashboardController, ShellController | StatsServiceTest |
| `StatsService.getReportMonths` | months with posts, the current month always first | ReportsController | StatsServiceTest |
| `StatsService.getReport` | every Section 3 metric for one month and the By City rows | ReportsController | StatsServiceTest |
| `PostDao.totalsByCity` | kg posted, rescued, expired and closed per city | StatsService | StatsServiceTest |
| `PostDao.sumKgListed` | kg on listings created in a period | StatsService | StatsServiceTest |
| `PostDao.countWithOffers` | posts that got at least one offer | StatsService | StatsServiceTest |
| `PostDao.countWithAcceptedOffer` | posts that had an offer accepted | StatsService | StatsServiceTest |
| `PostDao.findMonthsWithPosts` | every month that has a post | StatsService | StatsServiceTest |
| `OfferDao.countCompleted` | completed handoffs in a period | StatsService | StatsServiceTest |
| `OfferDao.countAccepted` | offers ever accepted in a period | StatsService | StatsServiceTest |
| `OfferDao.countLapsed` | offers that lapsed in a period | StatsService | StatsServiceTest |
| `ReportDao.sumPeopleServed` | people served in reports of a period | StatsService | StatsServiceTest |
| `ReportDao.countHandoffsPastDeadline` | handoffs whose report was due | StatsService | StatsServiceTest |
| `ReportDao.countReportedOnTime` | those reported on time | StatsService | StatsServiceTest |
| `RegistrationDao.countByStatus` | applications in one status | StatsService | StatsServiceTest |
| `AccountDao.countSuspended` | suspended accounts | StatsService | StatsServiceTest |

**Flow**

```mermaid
flowchart LR
  classDef screen fill:#e8f1ff,stroke:#3b6fd4,color:#111827
  classDef ctrl fill:#f1f5f9,stroke:#64748b,color:#111827
  classDef svc fill:#e8f7ee,stroke:#2f9e5a,color:#111827
  classDef dao fill:#fff4e0,stroke:#d98a00,color:#111827
  classDef tbl fill:#f3e8ff,stroke:#8a4fd1,color:#111827
  D([admin dashboard.fxml]):::screen --> DC[AdminDashboardController]:::ctrl --> AS{{StatsService<br/>getAdminSummary}}:::svc
  SH[ShellController<br/>sidebar badges]:::ctrl --> AS
  R([reports.fxml]):::screen --> RC[ReportsController]:::ctrl --> RM{{StatsService<br/>getReportMonths<br/>getReport}}:::svc
  AS --> RG[RegistrationDao<br/>countByStatus]:::dao --> TR[(registration)]:::tbl
  AS --> AC[AccountDao<br/>countSuspended]:::dao --> TA[(account)]:::tbl
  AS --> SN[SafetyNoticeDao<br/>countOpen]:::dao --> TN[(safety_notice)]:::tbl
  AS --> DE[DocumentDao<br/>findEndingBy]:::dao --> TD[(document)]:::tbl
  AS --> PD[PostDao<br/>totalsByCity<br/>sumKgListed]:::dao --> TP[(post)]:::tbl
  RM --> PD
  RM --> PC[PostDao<br/>countWithOffers<br/>countWithAcceptedOffer<br/>findMonthsWithPosts]:::dao --> TP
  RM --> OC[OfferDao<br/>countCompleted<br/>countAccepted<br/>countLapsed]:::dao --> TO[(offer)]:::tbl
  RM --> RP[ReportDao<br/>sumPeopleServed<br/>countHandoffsPastDeadline<br/>countReportedOnTime]:::dao --> TRP[(report)]:::tbl
```

### MAINT: automatic checks 🟡

Files: `service/MaintenanceService.java`, `controller/ShellController.java`, `util/Navigator.java`.

| Method | What it does | Called by | Test |
|---|---|---|---|
| `MaintenanceService.runMaintenance` | runs the 6.6 checks in order; today only the expiry suspension | ShellController | MaintenanceServiceTest, ShellMaintenanceTest |
| `AccountStatusService.suspendAccountsWithExpiredDocuments` | an Active account with a current verified expired document becomes Suspended ("Document expired") | MaintenanceService | AccountStatusTest, MaintenanceServiceTest |
| `DocumentDao.findCurrentExpiredRegistrations` | the accounts that query finds | AccountStatusService | AccountStatusTest |
| `Navigator.setBeforeScreenListener` | lets the shell run something before each in-shell screen loads | ShellController | ShellMaintenanceTest |

**Flow** (dashed = not built yet)

```mermaid
flowchart LR
  classDef screen fill:#e8f1ff,stroke:#3b6fd4,color:#111827
  classDef ctrl fill:#f1f5f9,stroke:#64748b,color:#111827
  classDef svc fill:#e8f7ee,stroke:#2f9e5a,color:#111827
  classDef dao fill:#fff4e0,stroke:#d98a00,color:#111827
  classDef tbl fill:#f3e8ff,stroke:#8a4fd1,color:#111827
  classDef planned fill:#ffffff,stroke:#94a3b8,stroke-dasharray:5 3,color:#475569
  NV[Navigator.show<br/>any in-shell screen]:::ctrl --> BL[beforeScreenListener]:::ctrl --> SC[ShellController<br/>runMaintenance]:::ctrl --> MS{{MaintenanceService<br/>runMaintenance}}:::svc
  SC -. a failing check is printed and skipped .-> NV
  MS -.-> P1[1 lapse accepted offers<br/>module 8]:::planned
  MS -.-> P2[2 expire listings<br/>module 5]:::planned
  MS -.-> P3[3 expire requests<br/>module 9]:::planned
  MS -.-> P4[4 expire offered offers<br/>module 6]:::planned
  MS -.-> P5[5 flag overdue reports<br/>module 10]:::planned
  MS --> ES{{AccountStatusService<br/>suspendAccountsWithExpiredDocuments}}:::svc
  subgraph TX[one transaction A2]
    FE[DocumentDao<br/>findCurrentExpiredRegistrations]:::dao
    SI[AccountDao<br/>suspendIfActive]:::dao
    LG[LogDao]:::dao
  end
  ES --> FE
  ES --> SI
  ES --> LG
  FE --> TD[(document)]:::tbl
  SI --> TA[(account)]:::tbl
  LG --> TL[(status_change)]:::tbl
  NV --> SL([the screen loads]):::screen
```

### LISTING and REQUEST: post food, browse the other side's board 🟡

Files: `service/PostService.java`, `dao/PostDao.java`, `dao/DetailDao.java`, `dao/PhotoDao.java`, `model/Post.java`, `model/FoodDetail.java`, `model/UseDetail.java`, `service/FileStorageService.java`. The screens (`NewListingController`, `NewRequestController`, `MyListingsController`, `MyRequestsController`, `BrowseListingsController`, `BrowseRequestsController`) are still placeholders; they are wired in the last step of round 3.

| Method | What it does | Called by | Test |
|---|---|---|---|
| `PostService.createListing` | a donor posts a listing: checks the rules, saves photos, then food detail, post and log in one transaction | **no screen yet** | PostServiceTest |
| `PostService.createRequest` | a food group posts a request, the same way | **no screen yet** | PostServiceTest |
| `PostService.getOpenListings` | Open listings for a food group, its city first, suspended donors hidden, no handover text | **no screen yet** | PostServiceTest |
| `PostService.getOpenRequests` | Open requests for a donor; a Casual donor sees only raw or packaged | **no screen yet** | PostServiceTest |
| `PostService.getMyPosts` | the logged-in account's own posts with offer counts | **no screen yet** | PostServiceTest |
| `PostDao.insert` | saves the post row for an inserted detail | PostService | PostServiceTest |
| `PostDao.findById` | one post with its detail | none yet: OFFER will use it | none |
| `PostDao.findOpenBoard` | Open posts of one kind from Active posters, viewer's city first | PostService | PostServiceTest |
| `PostDao.findByPoster` | one account's posts with their offer counts | PostService | PostServiceTest |
| `DetailDao.insertFood` | saves a food detail | PostService | PostServiceTest |
| `DetailDao.insertUse` | saves a use detail | PostService | PostServiceTest |
| `DetailDao.readFood` | reads the food columns of a joined row | PostDao | PostServiceTest |
| `DetailDao.readUse` | reads the use columns of a joined row | PostDao | PostServiceTest |
| `PhotoDao.insert` | attaches a stored photo path to a post (offers and reports reuse it) | PostService | PostServiceTest |
| `PhotoDao.findPaths` | the photo paths of one post | PostService | PostServiceTest |
| `FileStorageService.saveAllPhotos` | checks 1 to 5 files, then saves them; removes any it already copied if one fails | PostService | PostServiceTest |
| `FileStorageService.deleteAll` | removes saved files after a failed transaction | PostService | PostServiceTest |

```mermaid
flowchart LR
  classDef svc fill:#e8f1ff,stroke:#3b6fd4,color:#111827
  classDef dao fill:#fff4e5,stroke:#d9822b,color:#111827
  classDef tbl fill:#f1f5f9,stroke:#64748b,color:#111827
  classDef planned fill:#ffffff,stroke:#94a3b8,stroke-dasharray:4 3,color:#475569
  NL([New Listing, New Request<br/>not wired yet]):::planned -.-> CP{{PostService<br/>createListing<br/>createRequest}}:::svc
  CP --> FS[FileStorageService<br/>saveAllPhotos]:::svc
  CP --> DD[DetailDao<br/>insertFood<br/>insertUse]:::dao --> TD[(food_detail<br/>use_detail)]:::tbl
  CP --> PD[PostDao<br/>insert]:::dao --> TP[(post)]:::tbl
  CP --> PH[PhotoDao<br/>insert]:::dao --> TF[(photo)]:::tbl
  CP --> LG[LogDao<br/>logStatusChange]:::dao --> TL[(status_change)]:::tbl
  BR([Browse, My Listings, My Requests<br/>not wired yet]):::planned -.-> GB{{PostService<br/>getOpenListings<br/>getOpenRequests<br/>getMyPosts}}:::svc
  GB --> PQ[PostDao<br/>findOpenBoard<br/>findByPoster]:::dao --> TP
  GB --> PP[PhotoDao<br/>findPaths]:::dao --> TF
```

### SHELL: the app frame 🔧

Files: `controller/ShellController.java`, `view/shared/shell.fxml`, `util/Navigator.java`, `util/SvgIcon.java`, `view/aniplato.css`, `aniplato/Main.java`.

**Flow**

```mermaid
flowchart LR
  classDef screen fill:#e8f1ff,stroke:#3b6fd4,color:#111827
  classDef ctrl fill:#f1f5f9,stroke:#64748b,color:#111827
  classDef svc fill:#e8f7ee,stroke:#2f9e5a,color:#111827
  M[Main]:::ctrl --> NI[Navigator.init]:::ctrl --> NL([login.fxml]):::screen
  NL --> NS[Navigator.show SHELL]:::ctrl --> SH([shell.fxml]):::screen --> SC[ShellController<br/>sidebar, badges, assistant]:::ctrl
  SC --> NC[Navigator.setContent<br/>setScreenListener<br/>setBeforeScreenListener]:::ctrl
  NC --> IN([in-shell screens fill the content area]):::screen
  SC --> LO[handleLogout]:::ctrl --> AL{{AuthenticationService<br/>logout}}:::svc --> NL
```

| Method | What it does | Called by | Test |
|---|---|---|---|
| `Navigator.init` | gives Navigator the window's scene and wires every `nav-*` button | Main | ShellMaintenanceTest |
| `Navigator.show` | shows a screen: a window screen replaces the scene, an in-shell one fills the content area | most controllers | ShellMaintenanceTest |
| `Navigator.setContent` | tells Navigator where in-shell screens go | ShellController | ShellMaintenanceTest |
| `Navigator.setScreenListener` | lets the sidebar refresh its highlight after each navigation | ShellController | none |
| `Navigator.sidebarScreenFor` | which sidebar button a detail screen highlights | ShellController | none |
| `Navigator.currentRole` | the logged-in role | MyOffersController, HistoryController, HandoffRecordController | none |
| `Navigator.currentScreen` | the screen being shown | ShellController | none |
| `Navigator.key` | the `nav-<key>` name of a screen | Navigator | none |
| `SvgIcon.load` | loads an SVG icon as a node | DialogController, ShellController, ReviewRegistrationController | DialogControllerTest |

### POPUPS: dialogs 🔧

Files: `controller/shared/DialogController.java`, `controller/shared/dialog/` (all), `view/shared/dialog.fxml`, `view/shared/dialog/` (all). Sixteen dialogs; eight do a real action (Reject, Approve, Suspend, Reinstate, Resolve notice, Notice resolved, Set new password, Forgot password) and eight only open and close until their feature is built.

**Flow**

```mermaid
flowchart LR
  classDef screen fill:#e8f1ff,stroke:#3b6fd4,color:#111827
  classDef ctrl fill:#f1f5f9,stroke:#64748b,color:#111827
  classDef svc fill:#e8f7ee,stroke:#2f9e5a,color:#111827
  S([any screen]):::screen -- fx:include dialog.fxml --> DC[DialogController<br/>showXxxMode]:::ctrl
  DC -- opens one card, closes the rest --> CARD[the 16 DialogCard classes]:::ctrl
  CARD -- the 8 real dialogs only --> SV{{a service call}}:::svc
  SV -. rule fails, message shown .-> ER[DialogController.showError]:::ctrl
  CARD -- Back or Close --> DC
```

The `showXxxMode` methods are in `DialogController` and are outside the service/DAO coverage rule; section 6 lists the ones nothing calls yet. Test: `DialogControllerTest`.

### PLATFORM: foundations 🔧

Files: `util/AppConfig.java`, `util/Database.java`, `util/Session.java`, `util/Settings.java`, `util/PasswordHasher.java`, `service/EmailService.java`, `service/FileStorageService.java`, `dao/LogDao.java`, `exception/` (both), `sql/` (all), `pom.xml`.

**Flow**

```mermaid
flowchart LR
  classDef ctrl fill:#f1f5f9,stroke:#64748b,color:#111827
  classDef svc fill:#e8f7ee,stroke:#2f9e5a,color:#111827
  classDef dao fill:#fff4e0,stroke:#d98a00,color:#111827
  classDef tbl fill:#f3e8ff,stroke:#8a4fd1,color:#111827
  SV{{any service that writes}}:::svc --> TX[Database.inTransaction]:::ctrl --> GC[Database.getConnection]:::ctrl --> AC[AppConfig<br/>db.properties or a -Daniplato override]:::ctrl --> DB[(MySQL)]:::tbl
  TX -- an exception rolls everything back --> SV
  SV --> LG[LogDao<br/>logStatusChange]:::dao --> TL[(status_change)]:::tbl
  SV --> EM[EmailService]:::svc --> EL[(email_log)]:::tbl
  SV --> FS[FileStorageService]:::svc --> UP[(uploads.dir)]:::tbl
```

| Method | What it does | Called by | Test |
|---|---|---|---|
| `AppConfig.get` | a setting from `db.properties` (or a `-Daniplato.<key>` override), with a fallback | FileStorageService | TestDbTest |
| `AppConfig.required` | the same, but a missing value is a setup mistake | Database | TestDbTest |
| `Database.getConnection` | opens a connection | every service | every service test |
| `Database.inTransaction` | all-or-nothing: commit if the work finishes, roll back if anything throws (A2) | RegistrationService, AccountStatusService, DocumentReviewService, SafetyService | RegistrationDecisionTest (rollback) |
| `Session.start` | begins a session | AuthenticationService | AuthenticationServiceTest |
| `Session.clear` | ends it | AuthenticationService | AuthenticationServiceTest |
| `Session.isLoggedIn` | is anyone logged in | AuthenticationService | AuthenticationServiceTest |
| `Session.current` | the logged-in account or null | AuthenticationService, DocumentReviewService | DocumentReviewTest |
| `Session.require` | the logged-in account; nobody logged in is a bug | services, ProfileController, ShellController | every service test |
| `Session.requireAdmin` | the logged-in admin; anyone else is refused with a message | admin-only services | AccountStatusTest |
| `LogDao.logStatusChange` | one row in `status_change` for every status change (A3) | RegistrationService, DocumentReviewService, AccountStatusService, SafetyService | every decision test |
| `LogDao.logEmail` | one row in `email_log` per send attempt, with the error if it failed | EmailService | AuthenticationServiceTest |

### Model helpers

| Method | What it does | Called by | Test |
|---|---|---|---|
| `Account.displayName` | the role's name on screen (on the role enum) and the abstract name shown in headers | ShellController, ProfileController | none |
| `Account.key` | the role's `role-<key>` style class for the sidebar | ShellController | none |
| `Account.profileRows` | the rows the Profile screen lists (each subclass supplies its own) | ProfileController | none |
| `Account.isSuspended` | is this account Suspended | ProfileController | AccountTest |
| `Account.canPostOrOffer` | only an Active account may post, offer or accept (6.3) | PostService | AccountTest, PostServiceTest |
| `Admin.displayName` | the admin's name | ShellController | none |
| `Admin.profileRows` | the admin's profile rows | ProfileController | none |
| `Donor.displayName` | the donor's organization name | ShellController | none |
| `Donor.profileRows` | the donor's profile rows | ProfileController | none |
| `FoodGroup.displayName` | the food group's name | ShellController | none |
| `FoodGroup.profileRows` | the food group's profile rows | ProfileController | none |
| `Document.label` | the document type's name on screen | review and profile screens | none |
| `Document.requiresExpiry` | permits and the Barangay Certificate always need an expiry date | RegistrationService | RegistrationApplyTest |
| `Document.dbValue` | the lowercase value stored in the database | DocumentDao | RegistrationApplyTest |
| `Document.fromDb` | reads a stored value back into the enum | DocumentDao | RegistrationApplyTest |
| `Document.isExpired` | the expiry date is before today | RegistrationService, ProfileController | RegistrationDecisionTest |
| `Document.expiresWithin` | still valid but ending within N days | ProfileController | DocumentReviewTest |
| `Offer.label` | the offer status's name on screen | none yet | none |
| `Post.label` | the post status's name on screen (on the status enum) | none yet | none |
| `Post.isListing` | is this a listing (not a request) | Post.deadline | PostServiceTest |
| `Post.deadline` | the use-by of a listing or the needed-by of a request | none yet: the MAINT checks use SQL | none |
| `FoodDetail.label` | a category, storage or allergen name on screen | none yet | none |
| `FoodDetail.allowedForCasual` | Casual donors give raw or sealed-packaged only (6.5) | PostService | PostServiceTest |
| `FoodDetail.column` | the value stored in the allergens SET column | DetailDao | PostServiceTest |
| `FoodDetail.fromColumn` | reads a stored allergen back | DetailDao | PostServiceTest |
| `SafetyNotice.label` | "Notice #12" | SafetyNoticesController | none |

## 6. Reference C: methods and classes nothing calls yet

| Item | Why it exists | Decision |
|---|---|---|
| `AuthenticationService.mustChangePassword` | the login screen reads `Account.mustChangePassword` directly | **candidate for deletion**: say so in a pull request |
| `SafetyService.raise` | built and tested; the Flag Concern dialog is not wired | keep: FLOWS.md module 10 |
| `PostService.createListing`, `createRequest`, `getOpenListings`, `getOpenRequests`, `getMyPosts` | built and tested; the screens are wired in the last step of round 3 | keep: modules 5 and 9 |
| `Account.canPostOrOffer` | the 6.3 rule for posting, offering and accepting; PostService uses it, OFFER and ACCEPT will | keep: modules 6 and 7 |
| `Offer.label` | status text for the offer screens | keep: module 6 |
| `DialogController.showConfirmReleaseMode`, `showConfirmReceiptMode`, `showFlagSafetyConcernMode`, `showAcceptOfferMode`, `showWithdrawOfferMode`, `showUpgradeBusinessDonorMode` | their screens are not built, so no caller | keep: modules 4, 6, 7, 8, 10 |
| `ReportService` | empty class from the scaffold; spec 7.3 gives it the documentation report | keep: FLOWS.md module 10 |
| the eight placeholder dialogs | open and close only | each becomes real with its feature |
| placeholder controllers under `controller/donor`, `controller/foodgroup`, `controller/shared` | screens that exist in FXML with sample data | each becomes real with its feature |

## 7. Reference D: screens

Each `Navigator.Screen` with its FXML and controller. *Window* screens replace the whole scene; *in-shell* screens fill the shell's content area.

| Screen | FXML | Controller | Kind | Feature |
|---|---|---|---|---|
| LOGIN | `view/login.fxml` | LoginController | window | AUTH |
| REGISTER | `view/register.fxml` | RegisterController | window | REGISTER |
| REGISTER_STEP2 | `view/register-step2.fxml` | RegisterStep2Controller | window | REGISTER |
| REGISTER_SUBMITTED | `view/register-submitted.fxml` | RegisterSubmittedController | window | REGISTER |
| SHELL | `view/shared/shell.fxml` | ShellController | window | SHELL |
| PROFILE | `view/profile.fxml` | ProfileController | in-shell | PROFILE |
| ADMIN_DASHBOARD | `view/admin/dashboard.fxml` | AdminDashboardController | in-shell | STATS |
| PENDING_REGISTRATIONS | `view/admin/pending-registrations.fxml` | PendingRegistrationsController | in-shell | REVIEW |
| REVIEW_REGISTRATION | `view/admin/review-registration.fxml` | ReviewRegistrationController | in-shell | REVIEW |
| ACCOUNTS | `view/admin/accounts.fxml` | AccountsController | in-shell | ACCOUNTS |
| SAFETY_NOTICES | `view/admin/safety-notices.fxml` | SafetyNoticesController | in-shell | SAFETY |
| REPORTS | `view/admin/reports.fxml` | ReportsController | in-shell | STATS |
| DONOR_DASHBOARD | `view/donor/dashboard.fxml` | DonorDashboardController | in-shell | HISTORY |
| LISTING_FORM | `view/donor/new-listing.fxml` | NewListingController | in-shell | LISTING |
| MY_LISTINGS | `view/donor/my-listings.fxml` | shared MyListingsController | in-shell | LISTING |
| LISTING_OFFERS | `view/donor/incoming-offers.fxml` | IncomingOffersController | in-shell | ACCEPT |
| DONOR_BROWSE_REQUESTS | `view/donor/browse-request.fxml` | BrowseRequestsController | in-shell | OFFER |
| DONOR_MAKE_OFFER | `view/donor/make-offer.fxml` | donor MakeOfferController | in-shell | OFFER |
| DONOR_MY_OFFERS | `view/shared/my-offers.fxml` | shared MyOffersController | in-shell | OFFER |
| FOODGROUP_DASHBOARD | `view/foodgroup/dashboard.fxml` | FoodGroupDashboardController | in-shell | HISTORY |
| BROWSE_LISTINGS | `view/foodgroup/browse-listings.fxml` | BrowseListingsController | in-shell | OFFER |
| MAKE_OFFER | `view/foodgroup/make-offer.fxml` | foodgroup MakeOfferController | in-shell | OFFER |
| MY_OFFERS | `view/foodgroup/my-offers.fxml` | foodgroup MyOffersController | in-shell | OFFER |
| NEW_REQUEST | `view/foodgroup/new-request.fxml` | NewRequestController | in-shell | REQUEST |
| MY_REQUESTS | `view/foodgroup/my-requests.fxml` | MyRequestsController | in-shell | REQUEST |
| FOODGROUP_OFFERS_RECEIVED | `view/foodgroup/incoming-offers.fxml` | FoodGroupIncomingOffersController | in-shell | ACCEPT |
| FOODGROUP_SUBMIT_REPORT | `view/foodgroup/submit-report.fxml` | SubmitReportController | in-shell | REPORT |
| HISTORY | `view/shared/history.fxml` | HistoryController | in-shell | HISTORY |
| HANDOFF_RECORD | `view/shared/handoff-record.fxml` | HandoffRecordController | in-shell | HANDOFF |

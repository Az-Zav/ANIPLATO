# ANIPLATO map: what exists today

This is the code as it stands after the cleanup. It lists only things that are built. What is still to build, and how, is in [FLOWS.md](FLOWS.md). Check any line here by opening the file named.

**Reading a chain:** `Screen → Controller.handler → Service.method → Dao.method → table`. `⚠` means the service can throw `RuleViolationException`, which the screen shows as a message.

**Every chain is covered by a test** (the last column in each table). Run them with `mvn test`; they use their own `aniplato_test` database.

## 1. The layers in one picture

```
view/*.fxml ──► controller/*Controller ──► service/*Service ──► dao/*Dao ──► MySQL
 (layout)        (read input, show)         (every rule)         (SQL only)
                                               │
                              util/Database.inTransaction  (one transaction per action, A2)
                              dao/LogDao                   (status_change log, A3)
```

| Layer | Folder | What is in it now |
|---|---|---|
| Model | `model/` | Account (+ Admin, Donor, FoodGroup), Registration, Document, City, SafetyNotice, Offer (status only) |
| DAO | `dao/` | AccountDao, RegistrationDao, DocumentDao, CityDao, SafetyNoticeDao, OfferDao, PostDao, ReportDao, LogDao |
| Service | `service/` | AuthenticationService, RegistrationService, DocumentReviewService, AccountStatusService, SafetyService, StatsService, EmailService, FileStorageService |
| Controller | `controller/` | one per screen; admin screens are wired, donor and food group screens are not yet |
| Dialogs | `controller/shared/dialog/` + `view/shared/dialog/` | 15 dialogs, one controller and one FXML each, opened through `DialogController` |
| Util | `util/` | AppConfig, Database, Session, Navigator, Settings, PasswordHasher, SvgIcon, AdminSelection, RegistrationDraft |

## 2. Who owns which rule

| Service | Owns | Public methods |
|---|---|---|
| **AuthenticationService** | login, password rules, forgot password | `login`, `logout`, `mustChangePassword`, `changePassword`, `requestPasswordReset`, `generateTemporaryPassword` |
| **RegistrationService** | applying, and the admin's decision | `cities`, `checkDetails`, `submit`, `approve`, `reject`, `getPending`, `getForReview` |
| **DocumentReviewService** | verifying documents, document lists and warnings | `verifyDocument`, `undoVerification`, `getMyDocuments`, `getCurrentDocumentAlerts` |
| **AccountStatusService** | suspend, reinstate, expiry suspension, account list | `suspendAccount`, `reinstateAccount`, `suspend(Connection, …)`, `suspendAccountsWithExpiredDocuments`, `getAccounts` |
| **SafetyService** | safety notices | `raise`, `getNoticeRows`, `dismiss`, `suspendFlaggedAccount` |
| **StatsService** | every admin number (derived, never stored, A9) | `getAdminSummary`, `getReportMonths`, `getReport` |
| **EmailService** | the four emails and `email_log` | `sendReceived`, `sendApproved`, `sendNotApproved`, `sendPasswordReset` |
| **FileStorageService** | uploads (PDF, JPG, PNG, 5 MB) under `uploads.dir` | `validate`, `save`, `resolve` |

## 3. What works end to end

### Login and passwords (Stage 1, R1.7)
| Chain | Test |
|---|---|
| `login.fxml → LoginController.handleLogin → AuthenticationService.login ⚠ → AccountDao.findByEmail, findPasswordHash → Session.start` | AuthenticationServiceTest |
| temporary password → `DialogController.showSetNewPasswordMode → SetNewPasswordDialogController → AuthenticationService.changePassword ⚠ → AccountDao.updatePassword` → `Navigator.show(SHELL)` | AuthenticationServiceTest, DialogControllerTest |
| `profile.fxml → ProfileController.handleForgotPassword → DialogController.showForgotPasswordMode → ForgotPasswordDialogController → AuthenticationService.requestPasswordReset → EmailService.sendPasswordReset → AccountDao.setTemporaryPassword` (only after the email is sent). **The Forgot password link on the login screen is not wired yet** (commented out in `LoginController`). | AuthenticationServiceTest |
| `ProfileController.handleChangePassword → DialogController.showSetNewPasswordMode(null, …) → SetNewPasswordDialogController → AuthenticationService.changePassword` | AuthenticationServiceTest |
| `ShellController.handleLogout → AuthenticationService.logout → Session.clear` | AuthenticationServiceTest |

### Registering (Stage 1)
| Chain | Test |
|---|---|
| `register.fxml → RegisterController → RegistrationService.cities → CityDao.findAll` and `checkDetails ⚠ → AccountDao.existsByEmail` | RegistrationApplyTest |
| `register-step2.fxml → RegisterStep2Controller.handleConfirmSubmit → RegistrationService.submit ⚠` *(one transaction)* `→ FileStorageService.save, RegistrationDao.insert, DocumentDao.insert, LogDao` then `EmailService.sendReceived` after the commit | RegistrationApplyTest |

### The admin reviewing registrations (Stage 1)
| Chain | Test |
|---|---|
| `pending-registrations.fxml` (and the dashboard list) `→ RegistrationService.getPending ⚠ → RegistrationDao.findByStatus, countEarlierRejected` | RegistrationDecisionTest |
| `review-registration.fxml → ReviewRegistrationController → RegistrationService.getForReview → DocumentDao.findByRegistration` | RegistrationDecisionTest |
| verify / undo a document `→ DocumentReviewService.verifyDocument / undoVerification ⚠ → DocumentDao.markReviewed → LogDao` | DocumentReviewTest |
| `handleApprove → DialogController.showApproveMode → ApproveDialogController → RegistrationService.approve ⚠` *(one transaction)* `→ DocumentDao.markOk, RegistrationDao.approve, AccountDao.insert or moveToRegistration, LogDao` then `EmailService.sendApproved` | RegistrationDecisionTest |
| `handleReject → … RejectDialogController → RegistrationService.reject ⚠ → RegistrationDao.reject → LogDao` then `EmailService.sendNotApproved` | RegistrationDecisionTest |

### The admin managing accounts (6.3)
| Chain | Test |
|---|---|
| `accounts.fxml → AccountsController → AccountStatusService.getAccounts → AccountDao.findAllOrganizations, DocumentDao.findEndingBy` | AccountStatusTest |
| suspend / reinstate buttons `→ DialogController.showSuspendAccountMode / showReinstateAccountMode → AccountStatusService.suspendAccount / reinstateAccount ⚠ → AccountDao → LogDao` | AccountStatusTest, DialogControllerTest |
| `safety-notices.fxml → SafetyNoticesController → SafetyService.getNoticeRows → SafetyNoticeDao.findRows`; dismiss / suspend through `showResolveSafetyNoticeMode → SafetyService.dismiss / suspendFlaggedAccount ⚠` *(one transaction)* `→ AccountStatusService.suspend(Connection, …)` | SafetyServiceTest |

### Numbers (Section 3, A9)
| Chain | Test |
|---|---|
| `admin/dashboard.fxml → AdminDashboardController`, `ShellController → StatsService.getAdminSummary → RegistrationDao, PostDao, SafetyNoticeDao, DocumentDao, AccountDao` | StatsServiceTest |
| `reports.fxml → ReportsController → StatsService.getReportMonths, getReport → PostDao, OfferDao, ReportDao` | StatsServiceTest |

### The organization's own profile
| Chain | Test |
|---|---|
| `profile.fxml → ProfileController → DocumentReviewService.getMyDocuments`; banner `getCurrentDocumentAlerts → DocumentDao.findCurrentAlerts`; opens files through `FileStorageService.resolve` | DocumentReviewTest |

### Raising a safety concern
`SafetyService.raise ⚠ → OfferDao.findParties, findStatus → SafetyNoticeDao.insert → LogDao`. **No screen calls it yet**; the Flag Concern dialog is not wired (FLOWS.md module 10). Test: SafetyServiceTest.

## 4. The popups

`DialogController` is the only thing a screen talks to. It opens one of 15 cards and closes the others.

```
screen.fxml ── <fx:include fx:id="dialog" source="…/dialog.fxml"> ──► DialogController (open / close / showError)
                                                                          │ owns 15 included cards
dialog.fxml ── <fx:include source="dialog/reject.fxml"> … ──► RejectDialogController extends DialogCard
```

| Dialog | Controller | Does a real action now |
|---|---|---|
| Reject Org, Approve Org | `RejectDialogController`, `ApproveDialogController` | yes (RegistrationService) |
| Suspend, Reinstate, Resolve Safety Notice | `SuspendAccount…`, `ReinstateAccount…`, `ResolveSafety…` | yes, through a callback the screen passes in |
| Set New Password, Forgot Password | `SetNewPassword…`, `ForgotPassword…` | yes (AuthenticationService) |
| Cancel Listing, Confirm Release, Confirm Receipt, Flag Safety, Accept Offer, Withdraw Offer, Upload Renewal, Upgrade to Business Donor | one each | **no, they only open and close** until their feature is built |

To add a dialog: a card FXML in `view/shared/dialog/`, a `DialogCard` subclass, one `fx:include` in `dialog.fxml`, one `showXxxMode` in `DialogController`. `DialogControllerTest` and `FxmlWiringTest` fail if the wiring is wrong.

## 5. What is not built yet

Screens with `TODO(service)` and no service behind them: donor and food group dashboards, new listing, my listings, browse (both ways), make offer, my offers, incoming offers, new request, my requests, submit report, history, handoff record. The plan for each is in [FLOWS.md](FLOWS.md).

Known gaps in the built code are in [CLEANUP-REPORT.md](CLEANUP-REPORT.md).

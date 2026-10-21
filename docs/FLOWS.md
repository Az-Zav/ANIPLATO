# ANIPLATO Feature Flows

One block per feature: the call chain, then the files it touches. Build in this order. Spec stage numbers refer to `Project Spec.md`.
Notation: `Screen → Controller.handler → Service.method → Dao.method → table`. `⚠` = throws `RuleViolationException` (shown by `DialogController.showError`).

## Conventions
- **C1** A feature's chain is written here before its code.
- **C2** Touch only files in the chain. Create a file only when a chain needs it.
- **C3** Controller: one `@FXML` handler per button; read input → one service call → `Navigator.show` or bind result; `catch RuleViolationException` → `DialogController.showError`.
- **C4** Service method: validate (⚠) → open `Connection` → DAO calls → `LogDao.logStatusChange` if a status changes → commit when more than one table. `SQLException` → `DataAccessException`.
- **C5** DAO method: takes a `Connection`, `PreparedStatement`, no rules.
- **C6** Model: fields only for existing schema columns; status enums nested.
- **C7** Service = verb phrase (`sendOffer`); DAO = `findX` / `insert` / `updateX`.
- **C8** Rule ID in a comment only where the rule is enforced.
- **C9** One service calling another inside the same transaction passes its `Connection` (existing pattern: `RegistrationService.suspend(Connection, ...)`).

Tags: **[built]** exists and fits the schema · **[stale]** exists but targets the old `listing` model, rewrite · **[new]** does not exist yet.

## Screens (Figma file H5M7MWgWL1sqJMN3dZ8PyF, page "V2 (edited)", is the source of truth)
Figma frames that differ by role or account type are **variants of one shared screen** (A5: the screen never checks role; differences come from the Account subclass or the data). The spec's 7.2 tree is outdated; Figma wins.

| Shared screen | Figma variants it covers |
|---|---|
| login, set-new-password dialog | Log in · Set New Password |
| register, register-step2, register-submitted | Step 1 (Business / Casual / Food group) · Step 2 (3 variants) · Submitted |
| shell, dashboard | Admin / Donor / Food group dashboards · donor Suspended view · (AI chat: stretch) |
| profile | Admin / Donor (Business, Casual) / Food group profile · Upload Renewal / Upgrade dialogs |
| my-posts | Donor My Listings · Food group My Requests |
| post-offers | Donor Incoming Offers · Food group Offers Received |
| my-offers | Donor My Offers · Food group My Offers |
| browse | Food group Browse Listings · Donor Browse Requests (Business / Casual) |
| new-post | Donor New Listing (Business / Casual) · Food group New Request |
| make-offer | Food group Make Offer · Donor Make Offer |
| history, handoff-record, submit-report | History (donor / food group) · Handoff Record (donor / food group) · Submit Report |
| Admin only | pending-registrations · review-registration (Business / Casual) · accounts · safety-notices · reports |
| Dialogs (DialogController) | Cancel Post · Accept / Withdraw Offer · Flag Concern · Confirm Release / Receipt · Approve / Reject Org · Suspend / Reinstate / Resolve Notice |

Chains below name screens by their Figma role names; where a chain says "donor/..." or "foodgroup/..." read it as the shared screen above. Frame IDs are in the Figma metadata (e.g. `480:2472` My Listings).

---

# Integration Roadmap (build order, screen by screen)

Each module wires screens that already exist in FXML to the call chains below (the "Chain" column points at the numbered feature sections). We write only the backend pieces a module needs and adjust controllers as we go. `[FE]` = screen not yet delivered by the frontend.

| # | Module | Screens wired | Backend we write | Chain | Needs first |
|---|---|---|---|---|---|
| 1 | **Login** (in progress) | login, Set New Password dialog, shell, logout | done; fix issues found in click-through | 1, 11 | none |
| 2 | **Registration** | register, register-step2, register-submitted | wire `checkDetails`, `submit`, `cities`; real city list, file picker, expiry dates, consent, Received email | 2 | 1 |
| 3 | **Admin review** | pending-registrations, review-registration, approve/reject dialogs | `RegistrationService`: `listPending`, `getForReview`, `verifyDocument`, `approve`, `reject`; new DAO methods; document preview | 2 | 2 |
| 4 | **Profile, accounts** | profile, accounts, suspend/reinstate/renewal/upgrade dialogs | `updateProfile`, `uploadRenewal`, `submitUpgrade`, `suspend`, `reinstate`, `listAccounts` | 8 | 3 |
| 5 | **Post a listing** | new-listing (+ `listing-form`), my-listings, cancel dialog | `Post`/`FoodDetail`/`Photo` models, `PostService`, `PostDao`, `DetailDao`, `PhotoDao`; delete old `Listing*` | 3a | 4 |
| 6 | **Browse and offer** | browse-listings (food group), make-offer, my-offers (shared) | `OfferService.sendListingOffer`, `withdraw`, `listMine`; rewrite `OfferDao` | 4 | 5 |
| 7 | **Accept offer** | incoming-offers (`listing-offers`), accept/decline dialogs | `acceptOffer`, `declineOffer`, phone-reveal method (A7) | 5 | 6 |
| 8 | **Handoff** | confirm release/receipt dialogs, handoff-record, history | `confirmRelease`, `confirmReceipt`, `buildHandoffRecord`, `cancelPost` | 6 | 7 |
| 9 | **Requests (Route 2)** | new-listing as request, browse-request (donor), donor make-offer, `[FE]` my-requests, `[FE]` offers-received | `createRequest`, `sendRequestOffer`, `UseDetail`; reuses modules 5 to 8 | 3b, 4 | 8 |
| 10 | **Report and safety** | `[FE]` submit-report, flag-concern dialog, safety-notices, resolve dialog | `ReportService`, `ReportDao`; fix `SafetyNoticeDao.insert` (`kind` column) | 7 | 8 |
| 11 | **Dashboards, history, reports** | three dashboards, history, admin reports | `StatsService`; all values derived (A9) | 9 | 8, 10 |
| 12 | **Maintenance** | all screens (runs on screen load) | rewrite `MaintenanceService`, re-attach to the shell | 10 | 5 to 10 |
| 13 | **Seed and polish** | whole app | demo seed, README, AI chat (stretch) | 12 | all |

**Per-module loop**
1. Read the Figma frame text and the FXML `fx:id`s.
2. Add the missing service and DAO methods.
3. Wire the controller: read input, one service call, then navigate or bind the result.
4. Check the service headless against MySQL, then click through the screen.
5. Commit with the module number; push after the click-through passes.

**Order rules**
- A module starts only after the previous one passes its click-through.
- `[FE]` screens in modules 9 and 10 are blocked until delivered; modules 1 to 8 are not.
- Modules 9 and 10 mostly reuse earlier work.

---

## 1. Login (built; verify only) — Stage 1, R1.7
```
login.fxml → LoginController.handleLogin
  → AuthenticationService.login ⚠ → AccountDao.findByEmail, findPasswordHash → account
  → Session.start
  → mustChangePassword ? DialogController.showSetNewPasswordMode → AuthenticationService.changePassword → AccountDao.updatePassword
  → Navigator.show(SHELL)
```

## 2. Register and approve — Stage 1
```
register.fxml → RegisterController.handleNext
  → RegistrationService.checkDetails ⚠ (+ cities() → CityDao.findAll)
register-step2.fxml → RegisterStep2Controller.handleSubmit
  → RegistrationService.submit ⚠   [one transaction]
      → FileStorageService.save (per document)
      → RegistrationDao.insert → registration
      → DocumentDao.insert → document
      → EmailService.sendReceived → email_log
register-submitted.fxml

pending-registrations.fxml → PendingRegistrationsController → RegistrationService.listPending → RegistrationDao
review-registration.fxml → ReviewRegistrationController
  approve → RegistrationService.approve ⚠   [one transaction]
      → DocumentDao.updateReview, AuthenticationService.generateTemporaryPassword + PasswordHasher
      → AccountDao.insert (or move to new registration for upgrade) → RegistrationDao.updateStatus
      → LogDao.insertStatusChange → EmailService.sendApproved
  reject  → RegistrationService.reject ⚠ → RegistrationDao.updateStatus → LogDao → EmailService.sendNotApproved
```
**Status:** applying side [built] (`cities`, `checkDetails`, `submit`, `RegistrationDao.insert`, `DocumentDao.insert`, `EmailService`). Admin side [new]: `listPending`, `getForReview`, `verifyDocument`, `approve`, `reject`; `RegistrationDao.find*/updateStatus`, `DocumentDao.find*/updateReview`, `AccountDao.insert`, PDF/image preview of documents (PDFBox, 7.5). The admin controllers are UI-only stubs today.

## 3a. Post a listing (Route 1) — Stage 2A
```
donor/new-listing.fxml → NewListingController.handlePost
  → PostService.createListing ⚠   [one transaction]
      rules: account Active + documents current, kg>0, use-by future, window < use-by,
             1–5 photos, category allowed for role, storage set, attestation ticked
      → PostDao.insert → post
      → DetailDao.insertFood → food_detail
      → PhotoDao.insert (per photo) after FileStorageService.save → photo
      → LogDao.insertStatusChange
  → Navigator.show(MY_LISTINGS)

donor/my-listings.fxml → MyListingsController → PostService.listMine → PostDao.findByOwner
  (rows: Open "Review offers" → incoming-offers; Accepted "Confirm Release" → feature 6; "Cancel listing" → Cancel Post dialog → PostService.cancelPost)
```
**Status:** [new] `Post`, `FoodDetail` models; `PostService`, `PostDao`, `DetailDao`, `PhotoDao`. [stale] delete `Listing`, `ListingDao`, `ListingService` (query the removed `listing` table). `Offer`, `OfferDao`, `OfferService` are also stale and are rewritten in feature 4; until then `MaintenanceService` stays detached.

## 3b. Post a request (Route 2) — Stage 2B
```
foodgroup/new-request.fxml → NewRequestController.handlePost
  → PostService.createRequest ⚠   [one transaction]
      → PostDao.insert → DetailDao.insertUse → use_detail → PhotoDao.insert → LogDao
```
Reuses everything from 3a; adds `UseDetail`, `new-request` screen, and `foodgroup/my-requests.fxml` → `MyRequestsController` → `PostService.listMine` (rows: "Review offers", "Confirm Receipt", "Cancel request").

## 4. Browse and offer — Stage 3
```
foodgroup/browse-listings.fxml → BrowseListingsController → PostService.listOpenListings(viewer city first, hides suspended) → PostDao.findOpen
donor/browse-requests.fxml     → BrowseRequestsController → PostService.listOpenRequests (casual: raw/sealed only)

foodgroup/make-offer.fxml → FoodGroupMakeOfferController.handleSend
  → OfferService.sendListingOffer ⚠   [one transaction]
      rules: post Open, no existing offer, pickup inside window, 1–5 photos, attestation
      → OfferDao.insert → PhotoDao.insert → LogDao
donor/make-offer.fxml → DonorMakeOfferController.handleSend
  → OfferService.sendRequestOffer ⚠  (+ DetailDao.insertFood for the offered food)

donor/my-offers.fxml + foodgroup/my-offers.fxml → MyOffersController (x2) → OfferService.listMine → OfferDao.findByOfferer
  withdraw → OfferService.withdraw ⚠ → OfferDao.updateStatus → LogDao
```
New: `BrowseRequestsController`, `DonorMakeOfferController`, `FoodGroupMakeOfferController` (rename existing `MakeOfferController`).

## 5. Accept / decline an offer — Stage 4
```
donor/incoming-offers.fxml (Route 1) + foodgroup/offers-received.fxml (Route 2) → same service calls
  load    → OfferService.listForPost → OfferDao.findByPost
  accept  → OfferService.acceptOffer ⚠   [one transaction]
      → OfferDao.updateStatus(accepted) + decline all other Offered
      → PostDao.updateStatus(Accepted), agreed kg → LogDao (each change)
  decline → OfferService.declineOffer ⚠ → OfferDao.updateStatus → LogDao
phone reveal: OfferService.getContactPhone (only A7 path; owner/admin/accepted other side)
```

Suspended offerers' offers are filtered out of `listForPost` (Stage 4).

## 6. Handoff and receipt — Stage 5 (handoff part)
```
Confirm Release dialog, opened from donor my-listings (Route 1) and donor my-offers (Route 2)
  → OfferService.confirmRelease ⚠ → OfferDao.updateReleased → LogDao
Confirm Receipt dialog (shows "before confirming" checks), opened from foodgroup my-offers (Route 1) and my-requests (Route 2)
  → OfferService.confirmReceipt ⚠   [one transaction]
      → OfferDao.updateStatus(Completed) → PostDao.updateStatus(Rescued) → LogDao
shared/handoff-record.fxml → HandoffRecordController → OfferService.buildHandoffRecord
```
The handoff record number is `HR-year-offer_id`, derived, not stored. The "Submit report" button sits on the food group's `handoff-record.fxml` (shows "Due <date>"); the donor's record shows the submitted report read-only.
Cancel (Stage 6): `MyPostsController.handleCancel → PostService.cancelPost ⚠ [one transaction] → PostDao + OfferDao (decline/cancel) → LogDao`.

## 7. Report and safety notice — Stage 5 (report), 6.3
```
shared/submit-report.fxml → SubmitReportController.handleSubmit
  → ReportService.submit ⚠ → ReportDao.insert → PhotoDao.insert (deadline = receipt + Settings; late flagged by derive)
shared/handoff-record → ReportService.findForHandoff (read-only for donor)

flag concern (handoff-record) → SafetyService.raise ⚠ → SafetyNoticeDao.insert → LogDao
admin/safety-notices.fxml → SafetyNoticesController
  dismiss → SafetyService.dismiss ⚠ → SafetyNoticeDao.updateStatus
  suspend → SafetyService.suspendAccount → RegistrationService.suspend → AccountDao.updateStatus → LogDao → (email)
```
**Status:** [new] `Report` model, `ReportDao`, `ReportService`, `submit-report` screen. [stale] `SafetyNoticeDao.insert` omits the `kind` column (schema requires it) and `SafetyNotice` has no kind; fix when touched. `SafetyService.raise/getNotices/dismiss/suspendFlaggedAccount` [built], recheck against the schema. The report deadline and "submitted late" are derived, not stored (A9).

## 8. Admin accounts and profile — 6.3
```
admin/accounts.fxml → AccountsController
  list      → RegistrationService.listAccounts → AccountDao.findAll
  suspend   → RegistrationService.suspend ⚠ → AccountDao.updateStatus → LogDao
  reinstate → RegistrationService.reinstate ⚠ → AccountDao.updateStatus → LogDao
profile.fxml → ProfileController
  edit      → RegistrationService.updateProfile ⚠ → AccountDao/RegistrationDao
  renewal   → RegistrationService.uploadRenewal ⚠ → FileStorageService → DocumentDao.insert
  upgrade   → RegistrationService.submitUpgrade ⚠ → new Pending registration (same path as 2)
  password  → ProfileController → AuthenticationService.changePassword [built]
```
**Status:** only `suspend(Connection, ...)`, `suspendAccountsWithExpiredDocuments` and `getCurrentDocumentAlerts` exist; the rest is [new]. Reinstate on a verified renewal (6.3) needs an admin step to verify the renewal; the screen list does not show where that lives. Figma answers it: `accounts.fxml` shows "Renewal uploaded → Review", which opens `review-registration.fxml`. Each document there gets three checks plus Verified/Undo (`RegistrationService.verifyDocument`, `unverifyDocument`); Approve unlocks when all are verified. Profile has Upload Renewal and Upgrade to Business dialogs.

## 9. Dashboards, History, Reports — Section 3
```
*/dashboard.fxml (admin, donor, foodgroup) → *DashboardController → StatsService.forCurrentUser / forAdmin → PostDao/OfferDao/ReportDao aggregate queries
shared/history.fxml → HistoryController → StatsService.history → OfferDao/PostDao
admin/reports.fxml → ReportsController → StatsService.metrics
```
All values derived on read (A9), nothing stored.

## 10. Maintenance — 6.6
```
Shell load → MaintenanceService.runMaintenance (calls services only, A4), in order:
  OfferService.lapseOverdueAcceptedOffers → PostService.expireOverdue → OfferService.expireOverdueOffered
  → ReportService.flagOverdueReports (→ SafetyService) → RegistrationService.suspendAccountsWithExpiredDocuments
Profile banner → RegistrationService.getCurrentDocumentAlerts
```
Re-attach to `ShellController` only now (detached since Foundation). Existing `MaintenanceService` calls stale methods; it is rewritten here. Each service's lapse/expire method is added in the feature that owns that status (3a, 4, 5, 7, 8) and only wired here.

## 11. Shell, navigation, logout — A5
```
shared/shell.fxml → ShellController → Navigator.show(role dashboard) ; nav-* menu per role ; logout → AuthenticationService.logout → Session.clear → Navigator.show(LOGIN)
```
**Status:** [built]. Check only; add menu entries (Browse requests, New request, My offers) as features 3b and 4 land.

## 12. Demo data and finish
`sql/seed.sql` (one full loop per route, one expired post, one suspended account, per Spec section 5 Demo); README. The AI assistant (spec stretch) has Figma frames ("Donor Dashboard – AI chat open") but stays out of this plan until everything above works. The donor Suspended restricted view is part of feature 9 (dashboard).

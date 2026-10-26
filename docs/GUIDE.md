# ANIPLATO code guide

How the code is organised and how to change it. `docs/Project Spec.md` says what the app must do; `CLAUDE.md` has the rules
(A1–A9); this file says where each thing lives. The diagram of who calls whom is `docs/CALL-GRAPH.md`
(generated from the source by `python3 scripts/callgraph.py`).

## The shape

```
View (FXML)  →  Controller  →  Service  →  DAO  →  MySQL
```

| Layer | Does | Never |
|---|---|---|
| View (`view/`) | Layout and style, drawn from Figma | Hold logic |
| Controller | Read the fields, call **one** service, show the result | Call a DAO, hold a rule, check the user's role on a shared screen |
| Service | Every rule, every status change and its log row; one transaction per action | Know JavaFX |
| DAO | SQL for one table, `PreparedStatement` only | Hold a rule or call a service |

`ArchitectureTest` fails the build when a layer is skipped, SQL leaks out of `dao/`, a connection is opened outside
`Database`, a pop-up `Alert` appears, or a class passes 600 lines.

## What is where

```
src/main/java/com/aniplato/
  controller/            one class per screen, plus the small helpers every screen shares
    admin/ donor/ foodgroup/ shared/
    shared/DialogController + DialogSpec     every popup
    Inline            where a message to the user is shown
    FormSupport       run a service call, show a broken rule, read a number, format a date
    Cards, PostCards, TableRows, *Rows       build the rows the list screens fill in code
  service/               10 services (spec 7.3) + DetailRules (shared validation)
  dao/                   12 DAOs: one per table, plus HistoryDao for the History queries
  model/                 entities and their enums
  util/                  Database, Navigator, Session, Selection, Settings, SvgIcon, ...
  view/                  FXML + aniplato.css (admin/, donor/, foodgroup/, shared/)
src/test/                service rules, screens, popups, architecture
sql/                     schema.sql (the only copy), seed.sql, demo seeds
```

**Services** (all the rules): `AuthenticationService`, `RegistrationService` (apply, approve, reject; then suspend,
reinstate, documents, renewals, contact edit, upgrade), `PostService`, `OfferService` (send, withdraw, accept, decline,
release, receipt, the handoff record, contact numbers), `ReportService`, `SafetyService`, `StatsService` (every figure,
the dashboards and History), `MaintenanceService` (the 6.6 checks; calls other services only), `FileStorageService`,
`EmailService`.

**Screens.** Donor and food group have their own FXML (they are separate Figma frames with their own wording) but share a
controller where the logic is the same, so a controller never asks "which role am I":

| Controller | Screens |
|---|---|
| `BrowseController` | donor Browse Requests, food group Browse Listings (`PostService.getOpenBoard`) |
| `MyPostsController` | donor My Listings, food group My Requests |
| `IncomingOffersController` | donor Incoming Offers, food group Offers Received |
| `MyOffersController` | donor My Offers, food group My Offers |
| `MakeOfferController` | one per route: `donor/` offers on a request, `foodgroup/` on a listing |
| `HistoryController`, `HandoffRecordController`, `ProfileController` | one screen each, for every role |

`Navigator` is the only class that knows view paths. A button that only goes to another screen gets a `nav-<screen>`
style class and no handler; a button that does work calls a service and then `Navigator.show(...)`. The row a user opened
travels in `Selection` (`postId`, `offerId`, `registrationId`).

## Messages and popups

**A message is a label on the screen, never a system pop-up.** `Inline` decides which label:

- `Inline.error(text)` goes to the open dialog's own line if a dialog is open, otherwise to the bar above the screen.
- `Inline.success(text)` is the same in green. `Inline.notice(text)` shows after the next screen change, for
  "it worked, but the email could not be sent".
- Login, Register and Review Registration have their own label: `Inline.show(label, text)` / `hide` / `fade`.
- `FormSupport.attempt(action)` and `FormSupport.load(read, fallback)` run a service call and send a broken rule (A8) to `Inline`.

**A popup is a `DialogSpec`.** There is one dialog card (`view/shared/dialog.fxml`); `DialogController.open(spec)` draws a
spec into it. A spec is a heading, a subheading, content blocks and buttons:

```java
open(DialogSpec.of(Tone.RED, "Suspend this account?", orgName + " can still log in ...")
        .body(Block.reason("Reason (shown to the organization) *", "e.g., Safety notice #12 confirmed."))
        .action("✕ Suspend", Style.DANGER, 110, form -> onConfirm.accept(form.text("reason"))));
```

Blocks: `reason`, `text` (or password), `file`, `date`, `dateOrNoExpiry`, `consent`, `facts`, `checks`, `notice`, `hint`,
`link`. A button's action may throw `RuleViolationException`: the message appears in the dialog and it stays open;
otherwise it closes (call `form.keepOpen()` to stay open after success). **To add a popup, add a `showXxxMode` method to
`DialogController` that builds a spec.** No new FXML and no new controller.

## Adding a feature

1. Find the rule in spec section 6 and note its ID (R4.1, A2 ...).
2. **DAO:** the SQL method, taking a `Connection`.
3. **Service:** one public method per user action. Check the rules, write inside `Database.inTransaction`, call
   `LogDao.logStatusChange` for every status change, throw `RuleViolationException` for a broken rule. Prefer the service
   that already owns the data (the list above) over a new one.
4. **Controller:** read the fields, call that method through `FormSupport.attempt`, then navigate.
5. **Test:** a service test with `DbTest` (one test per rule); load a new screen in the screen tests.
6. A new number, limit or wording that may change goes in `util/Settings`, not in a literal.

A schema change is `sql/schema.sql`, the spec's 7.6 data model and the ER diagram, together.

## Running and testing

- Run: `mvn javafx:run` after `setup-db` (see `README.md`). Tests: `mvn test`; they use their own `aniplato_test`
  database, rebuilt from `sql/schema.sql` and `sql/seed.sql` before every test, and refuse any other database.
- Screen and popup tests need a desktop for JavaFX (`xvfb-run -a mvn test` on a server).
- On MariaDB 10.11 one test fails on the untouched code as well:
  `RegistrationDecisionTest.anApplicantRejectedBeforeIsMarkedRegisteredAgain`. It has not been run on MySQL 8 yet.
- To see a screen without a database, load its FXML in a small JavaFX program with `aniplato.css` and take
  `scene.snapshot`; do this before and after a visual change and compare the pictures.

## Things that bite

- **Do not delete a CSS class just because no FXML names it.** JavaFX's own controls apply classes such as `arrow`,
  `arrow-button`, `date-picker` and `viewport` themselves; check them against JavaFX's `modena.css` first.
- In FXML, several style classes are separated by commas (`styleClass="a, b"`); a space makes one class called "a b".
- A phone number leaves the database through one method only (`OfferService.getContactPhone`, A7).
- `MaintenanceService` changes data only by calling other services (A4).

## Where the code differs from spec 7.2 and 7.3

The spec's tree is the target; these differences are deliberate:

- `HistoryDao` exists beside the table DAOs because History reads across `post` and `offer`.
- The donor and food group screens that share a controller keep two FXMLs, so the spec's `my-posts.fxml`,
  `post-offers.fxml` and `my-offers.fxml` are each a pair of files; `PostOffersController` is `IncomingOffersController`,
  and `SubmitReportController` lives in `foodgroup/`.
- `DetailRules` (shared validation for the post and offer forms) is a helper beside the services.
- Dialogs are specs drawn into one card, which is what "all popups go through DialogController" asks for; the spec's
  `dialog.fxml` is that one card.

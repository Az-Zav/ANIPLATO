# **ANIPLATO PRD**

# ---

## **1\. Overview**

ANIPLATO is a desktop app (JavaFX and MySQL) that helps Metro Manila businesses give surplus food to food groups before it spoils. It supports SDG 2 (Zero Hunger) and SDG 12 (Responsible Consumption and Production).

* Organizations register with their permits, and an admin checks them and approves by email.

* Businesses post surplus food. Food groups in the same city send offers saying how they will use it.

* The donor accepts one offer, both sides confirm the handoff, and every listing ends with a recorded outcome.

* A read-only AI assistant answers questions about the app (stretch goal).

---

## **2\. Problem and Evidence**

Businesses throw away edible surplus while parishes, schools, orphanages and charities need it. Coordination runs on paper and personal contacts, so food spoils before it reaches anyone.

| \# | Source | Key finding |
| :---- | :---- | :---- |
| 1 | [IJSTEM study on food bank management, Laguna (2025)](https://iiari.org/wp-content/uploads/ijstem.v5.3.3617.pdf) | No data-driven redistribution system exists; many food banks still coordinate on paper. |
| 2 | [House Bill No. 2196, 20th Congress](https://docs.congress.hrep.online/legisdocs/basic_20/HB02196.pdf) | Tasks DSWD with linking food banks and LGUs into one distribution system. |
| 3 | [A Convergent Systems Approach to Food Loss and Waste in The Philippines](https://www.researchgate.net/publication/399716856_A_Convergent_Systems_Approach_to_Food_Loss_and_Waste_in_The_Philippines) | Names digital surplus redistribution as a key leverage point. |
| 4 | [Food loss and waste in the Philippines: a literature review](https://www.researchgate.net/publication/376952222_Food_loss_and_waste_in_the_philippines_a_literature_review) | Most businesses throw surplus away rather than donate it. |
| 5 | [World Resources Institute, Reducing Food Loss and Waste (2013)](http://pdf.wri.org/reducing_food_loss_and_waste.pdf) | Distance between surplus and food groups blocks redistribution. |
| 6 | [House Bill No. 6525, via Philippine News Agency](https://www.pna.gov.ph/articles/1267682) | Would require businesses to donate edible surplus; cites about 930,000 tons wasted yearly. |
| 7 | [Semeniano et al. (2025), Philippine Journal of Development, PIDS](https://doi.org/10.62986/pjd2025.49.2b) | 31.4% of households faced moderate to severe food insecurity in 2023\. |

## ---

## **3\. Objectives and Metrics**

* Get surplus food to food groups before it spoils.

* Let only verified organizations take part.

* Close every listing with an outcome: rescued, expired or cancelled.

| Metric | Definition |
| :---- | :---- |
| Rescue rate | kg rescued ÷ kg listed, over closed listings, excluding cancelled ones |
| Offer acceptance rate | Listings with an accepted offer ÷ listings with at least one offer |
| Lapse rate | Accepted offers that lapsed ÷ all accepted offers |

## ---

## **4\. Users**

| User | Who they are | What they need |
| :---- | :---- | :---- |
| Admin | Runs the platform | Verify registrations, handle safety notices and suspensions, and track results |
| Business donor | Restaurants, bakeries, canteens, caterers and groceries | A quick way to donate surplus instead of throwing it away |
| Food group | Parishes, schools, orphanages and charities | One place to find nearby surplus instead of relying on calls |

## 

## ---

## **5\. Scope**

**In scope:**

* Registration with documents, admin review, and results by email

* Listings, same-city offers, donor acceptance and a two-step handoff

* Profile with document renewal and password change

* Cancellation, lapse and expiry handling

* Safety notices and account suspension

* Rescue-rate report

* Read-only AI assistant (stretch)

**Out of scope:** delivery, selling food, farm surplus, maps, in-app notifications, a web version, and legal protection (ANIPLATO grants none; see 6.3).

**Demo:** one full loop from registration to a rescued listing, plus one expired listing and one suspended account.

---

# **6\. How ANIPLATO Works**

This section says what the app does, step by step. Each stage lists the law that applies to it under 'Legal basis'. Rule IDs (R1.1, R2.3...) can be used in code comments and test cases.

## **6.1 The Big Picture**

* Register and get checked by the admin

* Donor posts a listing

* Food groups send offers

* Donor accepts one offer

* Handoff: donor confirms release, then the food group confirms receipt

* Close: Rescued, Expired or Cancelled. A missed pickup closes the listing as Expired, and the poster can post the food again as a new listing

Safety notices and suspensions can happen at any time after step 4\.

## **6.2 Step by Step**

Every stage has the same parts: Who, Needs, Does, Result.

### **Stage 1: Register and get verified**

| Component | Detail |
| :---- | :---- |
| **Who** | New business or food group. |
| **Needs** | Nothing. No account exists yet. |
| **Does** | Fills in organization details, contact person and number, and uploads each required document with its expiry date. Agrees to the consent text. |
| **Result** | Registration is Pending. The user gets a 'Received' email. |
| **Required documents** | Business donor: DTI, SEC or CDA registration; Mayor's Permit; Sanitary Permit. FDA License to Operate only if the business makes or repacks packaged food. Food group: one credential (DSWD, PCNC, SEC non-stock, DepEd/CHED/TESDA, or a diocese endorsement for parishes) plus an authorization letter naming the contact person. |
| **Legal basis** | Mayor's Permit (yearly, needs DTI, SEC or CDA registration) and Sanitary Permit: required of every business donor (R1.1, R1.4). FDA License to Operate: only manufacturers and repackers of packaged food need one (R1.1). DSWD, PCNC, DepEd/CHED/TESDA, SEC: these register welfare groups, schools and religious bodies, so each food group uploads one credential (R1.1, R1.4). RA 10173 (Data Privacy Act): collect only what is needed, and government IDs are sensitive, so none are collected (R1.2, R1.3). |
| **Rules** | R1.1 Each document needs an expiry date. 'No expiry' is allowed only where the document truly has none. Permits always need a date. R1.2 No government IDs are collected. R1.3 Files are visible only to the organization and the admin. R1.4 The admin can approve only if every document is not expired and matches the organization's name and city. R1.5 Approve: the account is created and an email sends a temporary password. R1.6 Reject: the admin must write a reason. The reason is emailed. The organization may register again. R1.7 Temporary password: it has no time limit. On every login with a temporary password, the user is sent to the Change Password screen and cannot reach anything else until a new password is set. If the user does not set one, the temporary password stays valid and the same screen shows on the next login. |

### **Stage 2: Donor posts a listing**

| Component | Detail |
| :---- | :---- |
| **Who** | Donor, or a food group re-posting extra food. |
| **Needs** | Account is Active (not Suspended). All documents are current. |
| **Does** | Enters description, category (raw, cooked, baked, packaged), kg, use-by time and pickup window, and picks a storage condition. Allergen labels and handover instructions are optional. Ticks the donor attestation. |
| **Result** | Listing is Open in the poster's city. The attestation is saved with a timestamp. |
| **Legal basis** | **RA 9803** (Food Donation Act) covers apparently wholesome food but excludes milk products, tomato- or milk-based cooked food, and food already served. These are blocked by category and the attestation (R2.3). **RA 9803**, liability: good-faith donors are not liable except for gross negligence or intentional misconduct. The timestamped attestation records good faith. ANIPLATO itself grants no protection (R2.3). |
| **Rules** | **R2.1** kg must be above 0\. **R2.2** Use-by must be in the future. The pickup window must end before the use-by. **R2.3** The attestation must be ticked: the food is wholesome, labeled, donated in good faith, and has no milk products, no tomato- or milk-based cooked food, and no food already served. **R2.4** Handover instructions stay hidden until an offer is accepted. **R2.5** A Suspended account cannot post or re-post a listing (see 6.3). **R2.6** A food group that re-posts acts as the donor for that listing. It sees offers on it, accepts or declines them, confirms release and can cancel it (Stages 4 to 6). A group cannot offer on its own re-post. **R2.7** Allergen labels are optional and for information only: Gluten / Wheat, Dairy / Butter, Nuts / Peanuts, Eggs, Halal Ingredients. **R2.8** A storage condition is required: Ambient Dry (room temperature, 22 to 28 °C, sealed containers), Chilled Cold-Chain (kept at 4 °C or below, the group brings a cooler) or Hot Hold (kept above 60 °C, pick up within 90 minutes). Food groups see the allergen labels and storage condition on the listing and when making an offer. |

### Stage 3: Food groups send offers

| Component | Detail |
| :---- | :---- |
| **Who** | Food group. |
| **Needs** | Listing is Open and in the group's own city. The group has not already offered on it. The group account is Active (not Suspended). |
| **Does** | Sends one offer for the whole quantity. States how the food will be used, who benefits and how many, the location, and a pickup time. Ticks the group attestation. Up to 5 photos and a message are optional. |
| **Result** | Offer is Offered. The donor can see it. |
| **Legal basis** | RA 9803, donation: beneficiaries must not pay anything, so groups confirm free distribution and no resale (R3.4). RA 10173, photos: identifiable people are personal data, so photos need adult consent and show no identifiable children (R3.4). |
| **Rules** | R3.1 One offer per group per listing. R3.2 Pickup time must be inside the pickup window and before the use-by. R3.3 Only the donor and the group that sent an offer can see it. R3.4 The group attestation must be ticked: the food is given free, not sold, and handled safely. Photos show no identifiable children, and identifiable adults have consented. R3.5 A group can withdraw its offer any time before the donor decides. R3.6 An offer nobody answers expires at its pickup time. R3.7 A Suspended account cannot send an offer (see 6.3). |

### **Stage 4: Donor accepts one offer**

| Component | Detail |
| :---- | :---- |
| **Who** | Donor, or the food group that re-posted the listing. |
| **Needs** | Listing is Open with at least one Offered offer. The account is Active (not Suspended). |
| **Does** | Picks one offer and accepts it. Can also decline any single offer. |
| **Result** | That offer is Accepted. All other offers are Declined. The listing is Accepted. Both sides now see each other's contact number, and the group sees the handover instructions. |
| **Legal basis** | RA 10173: contact numbers are shared only when needed, so only between the two sides of an accepted offer (R4.2). |
| **Rules** | R4.1 Accepting one offer and declining the others happen together in one database transaction. If any part fails, nothing changes. R4.2 Contact numbers are shown only to the two sides of the accepted offer, until the handoff closes. R4.3 The donor can decline an Offered offer at any time before accepting one. The group sees it as Declined and cannot offer again on that listing (R3.1). R4.4 A Suspended account cannot accept an offer (see 6.3). |

### **Stage 5: Handoff**

| Component | Detail |
| :---- | :---- |
| **Who** | Donor first, then food group. |
| **Needs** | Offer is Accepted. |
| **Does** | At pickup, the donor confirms release. After that, the food group confirms receipt. |
| **Result** | Offer is Completed. Listing is Rescued. Rescued kg is the listing's kg. Both sides can view a handoff record. |
| **Legal basis** | RA 9803: certificates of acceptance and fitness come from DSWD and the Red Cross. ANIPLATO's handoff record is not one of them (R5.2). |
| **Rules** | R5.1 The food group can confirm receipt only after the donor has confirmed release. R5.2 The handoff record shows names, food, kg, times and stated use. It always says it is not a government certificate. |

**Stage 6: Close**

| Component | Detail |
| :---- | :---- |
| **Who** | System / Donor / Food Group |
| **Needs** | A listing has reached its use-by time, experienced a missed pickup, been cancelled by the donor, or completed both handoff steps. |
| **Result** | Listing ends in one of four states: Rescued, Expired, Cancelled, or Expired after a missed pickup (allowing re-posting). |
| **Legal basis** | RA 9803 (Food Donation Act) guidelines and liability tracking. Proper closure ensures compliance records and transparency for donated surplus. |
| **Rules** | R6.1 A donor can cancel an Open or Accepted listing. A reason is required. R6.2 If the listing was Open, all waiting offers are declined and each group sees the reason. R6.3 If the listing was Accepted, the accepted offer is Cancelled and its group sees the reason. R6.4 When a pickup is missed, the accepted offer becomes Lapsed. The listing becomes Expired. The poster can post the food again as a new listing, with the old details pre-filled, and every group, including the one that lapsed, can offer on it. Reports count the new listing separately, and the original counts as Expired. |

## **6.3 Account Status and Access**

An account has only two states: Active and Suspended. 'Limited' is not a state. It describes what a Suspended account can do.

Suspended access: a Suspended account cannot post or re-post a listing, send an offer, or accept an offer. Its Open listings are hidden from food groups, and its Offered offers are hidden from donors, until it is reinstated. Everything else works as normal, including finishing handoffs already accepted, flagging concerns, uploading renewals, and updating the profile.

**Account changes:**

* (none) → Active: admin approves registration, all documents current. Email with temporary password sent.

* Active → Suspended: admin suspends (reason written), or a document expires (automatic).

* Suspended → Active: admin reinstates (reason written), or a renewal is uploaded and verified for a document-expiry suspension.

**Registration changes:**

* (none) → Pending: user submits registration.

* Pending → Approved: admin approves. Account is created.

* Pending → Rejected: admin rejects with a reason. User may register again.

## **6.4 Status Tables**

**Listing changes:**

* Open → Accepted: donor accepts an offer. Other offers are declined.

* Open → Expired: use-by time passes.

* Open → Cancelled: donor cancels with a reason. All waiting offers are declined.

* Accepted → Rescued: donor confirms release, then group confirms receipt. Handoff record is created.

* Accepted → Expired: pickup time \+ 2 hours passes with no handoff. The offer lapses.

* Accepted → Cancelled: donor cancels with a reason. The accepted offer is cancelled.

**Offer changes:**

* (none) → Offered: group submits, and rules R3.1 and R3.2 pass.

* Offered → Withdrawn: group withdraws before the donor decides.

* Offered → Expired: pickup time passes with no answer.

* Offered → Declined: donor declines it, accepts another offer, or cancels the listing.

* Offered → Accepted: donor accepts this one. Contacts are revealed.

* Accepted → Completed: both sides confirm the handoff.

* Accepted → Lapsed: pickup time \+ 2 hours passes with no handoff. The listing expires.

* Accepted → Cancelled: donor cancels the listing. The reason is shown to the group.

## **6.5 Who Can Do What**

A Suspended account has the same permissions as its role, except it cannot post, re-post, send an offer or accept an offer.

* Donor: post a listing; cancel own listing; accept or decline an offer on own listing; confirm release; flag a safety concern; upload renewals and edit profile.

* Food group: see Open listings in own city; send, withdraw and view own offers; confirm receipt; re-post extra food; as the poster of a re-post, view, accept and decline its offers, confirm release and cancel it; flag a safety concern; upload renewals and edit profile.

* Admin: approve or reject registrations; dismiss notices; suspend and reinstate accounts; view the rescue-rate report.

* Nobody else can see an offer except the donor and the group that sent it.

## **6.6 Automatic Checks**

One function, `runMaintenance()`, runs every time a screen loads. It does these in order:

* Accepted offer with pickup time \+ 2 hours passed and no handoff: offer becomes Lapsed. The listing becomes Expired.

* Open listing with use-by passed: listing becomes Expired.

* Offered offer with pickup time passed: offer becomes Expired.

* Document expiring in 30 days or less: show a warning.

* Document expired: account becomes Suspended (reason 'Document expired').

## **6.7 Settings**

* Lapse grace period: 2 hours after pickup time

* Document expiry warning: 30 days before

* Photos per offer: up to 5

**Assumptions to check:**

- A document expiry suspends the account automatically.

- Offered offers from a Suspended group are hidden from donors and reappear on reinstatement.

- A Suspended donor's Open listings are hidden from food groups, and their Offered offers are kept and reappear on reinstatement.

- A lapsed listing closes as Expired. Posting the food again creates a new listing, and any group can offer on it.

# **7\. Architecture**

This section says how the app is built, so each rule in Section 6 has one home in the code. Rule IDs (A1, A2...) can be used in code comments and tests.

## **7.1 Overview**

JavaFX desktop app, MVC, MySQL over JDBC.

```
View (FXML)  →  Controller  →  Service  →  DAO  →  MySQL
                    ↑
             Model (entities + statuses, used by all layers)
```

* View: FXML and CSS drawn from Figma. No logic.

* Controller: one thin class per screen. Reads input, calls a service, shows the result.

* Service: holds the rules from Section 6\. The only layer allowed to change data.

* DAO: SQL only, one per table.

* Model: entities and their status enums.

## **7.2 Project Tree**

```
aniplato/
├── pom.xml                                   (must copy *.fxml and *.css from src/main/java)
├── README.md
├── docs/
│   └── ARCHITECTURE.md
├── sql/
│   ├── schema.sql
│   └── seed.sql
├── uploads/                                  (local file storage, git-ignored)
└── src/main/
    ├── java/com/aniplato/
    │   ├── Main.java
    │   │
    │   ├── model/                            (enums nested inside)
    │   │   ├── Account                       (abstract; Role, Status, OrgType)
    │   │   ├── Admin
    │   │   ├── Donor
    │   │   ├── FoodGroup
    │   │   ├── City
    │   │   ├── Registration                  (Status)
    │   │   ├── Document                      (Type, Review)
    │   │   ├── Listing                       (Status, Category, Storage, Allergen)
    │   │   ├── Offer                         (Status; holds photo paths)
    │   │   └── SafetyNotice                  (Status)
    │   │
    │   ├── view/                             (FXML + CSS, mirrors controller/)
    │   │   ├── aniplato.css
    │   │   ├── login.fxml
    │   │   ├── register.fxml
    │   │   ├── register-step2.fxml
    │   │   ├── register-submitted.fxml
    │   │   ├── profile.fxml
    │   │   ├── admin/
    │   │   │   ├── dashboard.fxml
    │   │   │   ├── pending-registrations.fxml
    │   │   │   ├── review-registration.fxml
    │   │   │   ├── accounts.fxml
    │   │   │   ├── safety-notices.fxml
    │   │   │   └── reports.fxml
    │   │   ├── donor/
    │   │   │   └── dashboard.fxml
    │   │   ├── foodgroup/
    │   │   │   ├── dashboard.fxml
    │   │   │   ├── browse-listings.fxml
    │   │   │   ├── make-offer.fxml
    │   │   │   └── my-offers.fxml
    │   │   └── shared/
    │   │       ├── shell.fxml
    │   │       ├── listing-form.fxml
    │   │       ├── my-listings.fxml
    │   │       ├── listing-offers.fxml
    │   │       ├── history.fxml
    │   │       ├── handoff-record.fxml
    │   │       └── dialog.fxml
    │   │
    │   ├── controller/                       (thin: one per screen)
    │   │   ├── ShellController               (sidebar, header, logout, assistant, banners)
    │   │   ├── LoginController
    │   │   ├── RegisterController            (step 1: organization)
    │   │   ├── RegisterStep2Controller       (step 2: documents and consent)
    │   │   ├── RegisterSubmittedController   (submitted confirmation)
    │   │   ├── ProfileController             (also: renewal upload dialog)
    │   │   ├── admin/
    │   │   │   ├── AdminDashboardController
    │   │   │   ├── PendingRegistrationsController
    │   │   │   ├── ReviewRegistrationController
    │   │   │   ├── AccountsController
    │   │   │   ├── SafetyNoticesController
    │   │   │   └── ReportsController
    │   │   ├── donor/
    │   │   │   └── DonorDashboardController
    │   │   ├── foodgroup/
    │   │   │   ├── FoodGroupDashboardController
    │   │   │   ├── BrowseListingsController
    │   │   │   ├── MakeOfferController
    │   │   │   └── MyOffersController
    │   │   └── shared/                       (same screen for donor and food group)
    │   │       ├── ListingFormController
    │   │       ├── MyListingsController
    │   │       ├── ListingOffersController
    │   │       ├── HistoryController
    │   │       ├── HandoffRecordController
    │   │       └── DialogController          (all popups, incl. password dialogs)
    │   │
    │   ├── service/                          (all the rules live here)
    │   │   ├── AuthenticationService
    │   │   ├── RegistrationService
    │   │   ├── ListingService
    │   │   ├── OfferService
    │   │   ├── SafetyService
    │   │   ├── ReportService
    │   │   ├── MaintenanceService
    │   │   ├── FileStorageService
    │   │   └── EmailService
    │   │
    │   ├── dao/                              (one per table, SQL only)
    │   │   ├── CityDao
    │   │   ├── AccountDao
    │   │   ├── RegistrationDao
    │   │   ├── DocumentDao
    │   │   ├── ListingDao
    │   │   ├── OfferDao                      (includes offer photo paths)
    │   │   ├── SafetyNoticeDao
    │   │   └── LogDao                        (status-change log + email log)
    │   │
    │   ├── exception/
    │   │   └── RuleViolationException
    │   │
    │   └── util/
    │       ├── Database                      (connection + transaction helper)
    │       ├── PasswordHasher                (BCrypt)
    │       ├── Settings                      (2h grace, 30-day warning, 5 photos)
    │       ├── Session                       (who is logged in)
    │       ├── SvgIcon                       (loads bundled sidebar SVG assets)
    │       └── Navigator                     (switches screens; only place that knows view paths; see docs/FXML-GUIDE.md)
    │
    └── resources/
        ├── db.properties                     (git-ignored)
        └── email/
            ├── received.html
            ├── approved.html
            └── not-approved.html
```

## **7.3 Services**

### **AuthenticationService**

* Checks email and password (BCrypt).

* Blocks login until the temporary password is replaced (R1.7).

* Changes passwords: at least 8 characters and different from the current one.

### **RegistrationService**

* Saves registrations and uploaded documents. Sends the "Received" email (R1).

* Approve: one transaction creates the account and temporary password, then emails it. Reject: saves the reason and emails it (R1.5, R1.6).

* Handles renewals, document verification and profile edits (R8).

* Suspends and reinstates accounts, with a reason (R7.2, R8.4).

### **ListingService**

* Creates listings and re-posts. Validates kg, use-by, pickup window, storage condition and attestation (R2).

* Cancels listings and settles their offers (R6.1 to R6.3).

* Queries: open listings in the user's city (hiding suspended donors'), and a user's own listings.

### **OfferService**

* Sends, withdraws and declines offers (R3, R4.3).

* Accepts an offer: one transaction accepts it, declines the rest and marks the listing Accepted (R4.1).

* Confirms release, then receipt. One transaction sets the offer Completed and the listing Rescued (R5.1).

* Builds the handoff record (R5.2).

* Returns contact numbers to allowed viewers only (R4.2).

### **SafetyService**

* Saves safety notices on accepted or completed offers (R7.1).

* Admin dismisses a notice or suspends the account responsible, with a note, by calling RegistrationService.

### **ReportService**

* Computes rescue rate, acceptance rate, lapse rate, kg totals and per-city figures.

* Builds History for any user: closed listings they posted and closed offers they made.

### **MaintenanceService**

* Runs on every screen load, in the order of 6.6.

* Finds what is overdue and asks the owning service to change it. It holds no rules of its own.

### **FileStorageService**

* Saves uploads (PDF, JPG, PNG; max 5 MB) under `uploads/` and returns the relative path stored in the database.

### **EmailService**

* Fills the HTML templates, sends through Jakarta Mail and records every send in the email log.

## **7.4 Rules**

* A1. Controllers never call DAOs and hold no rules. They call services and show results.

* A2. An action that changes more than one table (accept, handoff, cancel, approve) is one service method and one transaction. If any part fails, nothing changes.

* A3. Every status change goes through its owning service, which also writes the status-change log.

* A4. MaintenanceService changes data only by calling other services.

* A5. Shared screens (My Listings, Listing Offers, History, Profile) never check the user's role. They ask for "my" data, and role differences come from the Account subclass or from what the data contains.

* A6. All popups go through DialogController with their own wording.

* A7. List queries never include phone numbers. One method returns them, only to the owner, an admin, or the other side of an accepted, not yet closed offer (R4.2).

* A8. Rule problems are thrown as `RuleViolationException` and shown to the user as a message.

* A9. Store statuses, timestamps and entered facts. Derive rates, totals, counts and expiry warnings.

## **7.5 Technical Choices**

* Database: MySQL over direct JDBC.

* Passwords: BCrypt.

* Email: Jakarta Mail.

* Files: local disk with relative paths in the database. PDFBox for PDF preview.

* Build: Maven. `pom.xml` must also copy `.fxml` and `.css` from `src/main/java`, or screens fail to load at startup.

* Settings: lapse grace 2 hours, expiry warning 30 days, up to 5 photos (6.7).

## **7.6 Data Model**

Ten tables. PK is the primary key and FK a foreign key. Organization details live only in registration, and the account holds the login.

* A registration has many documents. An approved registration has one account.

* A listing is posted by an account. An offer belongs to a listing and to a food group account.

* A safety notice belongs to an offer. Offer photos belong to an offer.

* Not stored, always worked out: rescued kg, all rates and totals, the handoff record number (HR-year-offer id), the "registered again" badge, document warnings, and the document in force (the latest one with review OK for each type).

### **1\. city**

Lookup table of Metro Manila's cities.

| Field | Type | Notes |
| :---- | :---- | :---- |
| city\_id | INT, PK |  |
| name | VARCHAR(60), UNIQUE | 16 cities \+ Pateros, seeded once |

### **2\. registration**

The organization's details, from submission onward.

| Field | Type | Notes |
| :---- | :---- | :---- |
| registration\_id | INT, PK |  |
| role | ENUM | DONOR, FOOD\_GROUP |
| org\_name | VARCHAR(120) |  |
| org\_type | ENUM | restaurant, bakery, canteen, caterer, grocery, parish, school, orphanage, charity |
| city\_id | INT, FK |  |
| contact\_person | VARCHAR(100) |  |
| contact\_phone | VARCHAR(20) |  |
| email | VARCHAR(120) | not unique, so a rejected org can register again |
| status | ENUM | PENDING, APPROVED, REJECTED |
| rejection\_reason | VARCHAR(500), null | emailed on reject |
| reviewed\_by | INT, FK account, null |  |
| reviewed\_at | DATETIME, null |  |
| submitted\_at | DATETIME | sorts Pending Accounts |

### **3\. account**

The login only. Organization details are read from the registration.

| Field | Type | Notes |
| :---- | :---- | :---- |
| account\_id | INT, PK |  |
| email | VARCHAR(120), UNIQUE | login |
| password\_hash | VARCHAR(60) | BCrypt |
| role | ENUM | ADMIN, DONOR, FOOD\_GROUP |
| status | ENUM | ACTIVE, SUSPENDED |
| suspend\_reason | VARCHAR(500), null | shown to the organization |
| must\_change\_password | BOOLEAN | true until the temporary password is replaced (R1.7) |
| password\_changed\_at | DATETIME | profile: Last changed |
| registration\_id | INT, FK, UNIQUE, null | empty for admin |
| admin\_name | VARCHAR(120), null | admin only |
| created\_at | DATETIME | admin's Since date |

### **4\. document**

One uploaded file. A renewal is a new row.

| Field | Type | Notes |
| :---- | :---- | :---- |
| document\_id | INT, PK |  |
| registration\_id | INT, FK |  |
| doc\_type | ENUM | dti, sec, cda, mayors\_permit, sanitary\_permit, fda\_lto, dswd, pcnc, sec\_non\_stock, deped\_ched\_tesda, diocese\_endorsement, authorization\_letter |
| file\_path | VARCHAR(255) | relative path under `uploads/` |
| expires\_on | DATE, null | empty \= No expiry |
| review | ENUM, null | OK, FLAGGED; empty \= awaiting review |
| flag\_reason | VARCHAR(500), null | goes into the rejection email |
| uploaded\_at | DATETIME |  |
| reviewed\_by | INT, FK account, null |  |
| reviewed\_at | DATETIME, null |  |

### **5\. listing**

One posted surplus. A food group re-posting also creates a listing.

| Field | Type | Notes |
| :---- | :---- | :---- |
| listing\_id | INT, PK |  |
| posted\_by | INT, FK account | donor, or a food group re-posting |
| city\_id | INT, FK | copied from the poster at posting |
| food\_description | VARCHAR(200) |  |
| category | ENUM | RAW, COOKED, BAKED, PACKAGED |
| storage | ENUM | AMBIENT\_DRY, CHILLED, HOT\_HOLD |
| allergens | SET, null | gluten, dairy, nuts, eggs, halal |
| kg | DECIMAL(6,2) | must be above 0 |
| use\_by | DATETIME |  |
| pickup\_start | DATETIME |  |
| pickup\_end | DATETIME | must be before use\_by |
| handover\_instructions | VARCHAR(500), null | hidden until an offer is accepted |
| donor\_attested\_at | DATETIME | the attestation tick |
| status | ENUM | OPEN, ACCEPTED, RESCUED, EXPIRED, CANCELLED |
| cancel\_reason | VARCHAR(500), null |  |
| created\_at | DATETIME |  |

### **6\. offer**

One food group's offer on one listing.

| Field | Type | Notes |
| :---- | :---- | :---- |
| offer\_id | INT, PK | also the handoff record number (HR-year-id) |
| listing\_id | INT, FK |  |
| group\_id | INT, FK account |  |
| use\_description | VARCHAR(300) | what the food is for |
| beneficiaries | VARCHAR(200) | who it feeds |
| headcount | INT |  |
| place | VARCHAR(200) |  |
| pickup\_time | DATETIME | inside the pickup window |
| message | VARCHAR(500), null |  |
| group\_attested\_at | DATETIME |  |
| status | ENUM | OFFERED, ACCEPTED, COMPLETED, DECLINED, WITHDRAWN, EXPIRED, LAPSED, CANCELLED |
| released\_at | DATETIME, null | donor confirms release |
| received\_at | DATETIME, null | group confirms receipt |
| created\_at | DATETIME |  |
| (constraint) | UNIQUE(listing\_id, group\_id) | one offer per group per listing (R3.1) |

### **7\. offer\_photo**

Optional photos on an offer.

| Field | Type | Notes |
| :---- | :---- | :---- |
| photo\_id | INT, PK |  |
| offer\_id | INT, FK |  |
| file\_path | VARCHAR(255) | up to 5 per offer, checked by the service |

### **8\. safety\_notice**

A concern raised on an accepted or completed offer.

| Field | Type | Notes |
| :---- | :---- | :---- |
| notice\_id | INT, PK | shown as Notice \#12 |
| offer\_id | INT, FK |  |
| raised\_by | INT, FK account | the flagged account is the other side of the offer |
| concern | VARCHAR(500) |  |
| status | ENUM | OPEN, DISMISSED, ACCOUNT\_SUSPENDED |
| admin\_note | VARCHAR(500), null | required on resolve |
| resolved\_by | INT, FK account, null |  |
| resolved\_at | DATETIME, null |  |
| raised\_at | DATETIME |  |

### 

### **9\. status\_change**

Append-only log of every status change.

| Field | Type | Notes |
| :---- | :---- | :---- |
| change\_id | INT, PK |  |
| entity\_type | ENUM | REGISTRATION, ACCOUNT, DOCUMENT, LISTING, OFFER, NOTICE |
| entity\_id | INT |  |
| from\_status | VARCHAR(30), null | empty for a new record |
| to\_status | VARCHAR(30) |  |
| reason | VARCHAR(500), null |  |
| changed\_by | INT, FK account, null | empty \= the system (maintenance) |
| changed\_at | DATETIME |  |

### **10\. email\_log**

Every email the app sends.

| Field | Type | Notes |
| :---- | :---- | :---- |
| email\_id | INT, PK |  |
| to\_email | VARCHAR(120) |  |
| kind | ENUM | RECEIVED, APPROVED, NOT\_APPROVED |
| sent\_at | DATETIME |  |
| success | BOOLEAN |  |
| error | VARCHAR(300), null |  |

**Assumptions to check:**

* Database credentials sit on each computer. Acceptable for a prototype only.

* Files on local disk are not shared between computers.

---

# **Appendix A. App Wording and Settings**

* **Registration consent:** "I agree that ANIPLATO may store my organization's details and documents to verify my organization. Only my organization and the ANIPLATO admin can see my files."

* **Donor attestation:** "This food is apparently wholesome, properly labeled and donated in good faith. It contains no milk products, no tomato- or milk-based cooked food, and no food already served to diners."

* **Group attestation:** "We will give this food away free, will not sell it, and will handle it safely after pickup. Photos show no identifiable children, and identifiable adults have consented."

* **Safety notice:** "The other party and the ANIPLATO admin will be notified. The admin may dismiss the notice or suspend the account responsible."

* **Handoff record:** "This is an ANIPLATO platform record, not a certificate issued by DSWD, the Philippine Red Cross or any government agency."

* **Emails:** Received (confirms the submission); Approved (temporary login, set a password on first login); Not approved (the admin's reasons, register again).

* **Settings:** lapse grace period 2 hours; expiry warning 30 days; temporary password has no time limit (R1.7).

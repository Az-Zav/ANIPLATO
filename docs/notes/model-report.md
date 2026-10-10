# Model layer report

Package `com.aniplato.model`. Ten classes. Models hold data and small derived helpers only. They have no SQL and no rule checks: rules live in services (A1, A2), SQL in DAOs.

Conventions used everywhere:
- Plain classes with a no-arg constructor, getters and setters (DAOs fill them from a `ResultSet`).
- Dates are `LocalDateTime` / `LocalDate`. Nullable columns are `null` (`Integer` for nullable foreign keys).
- Enums are nested in the class they belong to. Enums whose DB value is lowercase expose `dbValue()` and, where needed, `fromDb(String)`.
- "Display only" fields are not columns. A DAO fills them from a joined query so screens do not need a second lookup.

---

## City
Lookup row for a Metro Manila city.

| Member | Meaning |
|---|---|
| `id` | `city.city_id` |
| `name` | `city.name` |
| `toString()` | returns the name, so a ComboBox shows it directly |
| `equals` / `hashCode` | by `id`, so ComboBox selection matches by identity of the row |

## Registration
An organization's details, from submission onward. The login lives in `Account`.

| Field | Column / meaning |
|---|---|
| `id` | `registration_id` |
| `role` | DONOR or FOOD_GROUP (reuses `Account.Role`; never ADMIN) |
| `orgName`, `orgType` | organization name and type (`Account.OrgType`) |
| `city` | a `City` object, not just an id |
| `contactPerson` | contact person's name |
| `contactPhone` | contact number. **`null` in list queries** (rule A7); only one dedicated method fills it |
| `email` | contact email. Not unique, so a rejected org can register again |
| `status` | `Registration.Status`: PENDING, APPROVED, REJECTED |
| `rejectionReason` | the reason emailed on rejection |
| `reviewedBy`, `reviewedAt` | admin account id and time of the decision |
| `submittedAt` | sorts the Pending Accounts list |
| `documents` | the uploaded documents; filled by the DAO only when a screen needs them |

No behavior. It is a data holder.

## Document
One uploaded file. A renewal is a new row.

| Member | Meaning |
|---|---|
| `Type` enum | 12 document types. Each has `label()` (screen text, e.g. "Mayor's Permit"), `dbValue()` (lowercase DB value) and `fromDb()` |
| `Review` enum | OK, FLAGGED. A `null` review means awaiting review |
| `id`, `registrationId` | row id and owning registration |
| `type`, `filePath` | document type; path relative to `uploads/` |
| `expiresOn` | `null` means "No expiry" |
| `review`, `flagReason` | admin's verdict; the reason goes into the rejection email |
| `uploadedAt`, `reviewedBy`, `reviewedAt` | audit fields |
| `isExpired(today)` | true when `expiresOn` is before `today`. Never true if there is no expiry. Used to suspend accounts |
| `expiresWithin(days, today)` | still valid but expires within `days` (the 30-day warning). Not stored (A9) |
| `isAwaitingReview()` | `review == null` |

## Account (abstract)
The login. Every account is exactly one of `Admin`, `Donor`, `FoodGroup`.

| Member | Meaning |
|---|---|
| `Role` enum | ADMIN, DONOR, FOOD_GROUP |
| `Status` enum | ACTIVE, SUSPENDED. "Limited" is not a state; it describes what a Suspended account can do |
| `OrgType` enum | 9 organization types; has `dbValue()` / `fromDb()` |
| `id`, `email` | account id; login email (unique) |
| `passwordHash` | BCrypt hash |
| `status`, `suspendReason` | current status; reason shown to the organization |
| `mustChangePassword` | true until the temporary password is replaced (R1.7) |
| `passwordChangedAt`, `createdAt` | profile "Last changed"; admin's "Since" |
| `isSuspended()` | `status == SUSPENDED` |
| `role()` | abstract. Which kind of account this is |
| `displayName()` | abstract. Name shown in the header and lists |
| `profileRows()` | abstract. Ordered label/value pairs, so the shared Profile screen never checks the role (A5) |
| `requiredDocuments()` | abstract. A list of requirement slots; each slot is a set of types, satisfied by uploading any one of them |
| `canMakeOffers()` | abstract. Whether this account may send offers |
| `orgProfileRows(r)` | shared helper for donors and food groups: organization, type, city, contact person, phone, email, password last changed |

## Admin
Platform admin. No registration, so no organization details.

| Member | Meaning |
|---|---|
| `adminName` | `account.admin_name` |
| `role()` | ADMIN |
| `displayName()` | `adminName` |
| `profileRows()` | Name, Email, Since |
| `requiredDocuments()` | empty list |
| `canMakeOffers()` | false |

## Donor
Business that posts surplus.

| Member | Meaning |
|---|---|
| `registration` | the organization details (set by the DAO) |
| `role()` | DONOR |
| `displayName()` | organization name |
| `profileRows()` | organization profile rows |
| `requiredDocuments()` | (DTI **or** SEC **or** CDA), Mayor's Permit, Sanitary Permit |
| `FDA_LICENSE` | constant slot for the FDA License to Operate. Not in `requiredDocuments()` because only makers and repackers of packaged food need it (R1.1); the admin decides case by case |
| `canMakeOffers()` | false |

## FoodGroup
Parish, school, orphanage or charity.

| Member | Meaning |
|---|---|
| `registration` | the organization details (set by the DAO) |
| `role()` | FOOD_GROUP |
| `displayName()` | organization name |
| `profileRows()` | organization profile rows |
| `requiredDocuments()` | one credential (DSWD, PCNC, SEC non-stock, DepEd/CHED/TESDA, or diocese endorsement) plus an authorization letter |
| `canMakeOffers()` | true |

Note: a food group that re-posts food acts as the donor for that listing (R2.6). That is decided by who posted the listing, not by this class.

## Listing
One posted surplus.

| Member | Meaning |
|---|---|
| `Status` enum | OPEN, ACCEPTED, RESCUED, EXPIRED, CANCELLED |
| `Category` enum | RAW, COOKED, BAKED, PACKAGED |
| `Storage` enum | AMBIENT_DRY, CHILLED, HOT_HOLD; `label()` gives "Ambient Dry", "Chilled Cold-Chain", "Hot Hold" |
| `Allergen` enum | GLUTEN, DAIRY, NUTS, EGGS, HALAL; `label()` gives the R2.7 wording; `dbValue()` lowercase |
| `Allergen.parseDb(String)` | turns a SET value such as `"gluten,eggs"` into an `EnumSet`; null or empty gives an empty set |
| `Allergen.toDb(Set)` | the reverse; an empty set gives `null` |
| `id`, `postedBy`, `cityId` | listing id; poster's account id; city copied from the poster at posting |
| `foodDescription`, `category`, `storage` | what the food is and how it must be kept |
| `allergens` | informational labels (R2.7) |
| `kg` | `BigDecimal`; must be above 0 (checked by the service and by a DB constraint) |
| `useBy`, `pickupStart`, `pickupEnd` | the time window; pickup must end before use-by |
| `handoverInstructions` | hidden until an offer is accepted (R2.4) |
| `donorAttestedAt` | timestamp of the donor attestation tick |
| `status`, `cancelReason`, `createdAt` | lifecycle fields |
| `posterName`, `cityName` | **display only**, from joins |

## Offer
One food group's offer on one listing.

| Member | Meaning |
|---|---|
| `Status` enum | OFFERED, ACCEPTED, COMPLETED, DECLINED, WITHDRAWN, EXPIRED, LAPSED, CANCELLED |
| `id` | offer id; also the handoff record number |
| `listingId`, `groupId` | listing and the food group account |
| `useDescription`, `beneficiaries`, `headcount`, `place` | what the food is for, who it feeds, how many, where |
| `pickupTime` | must fall inside the listing's pickup window |
| `message` | optional note |
| `groupAttestedAt` | timestamp of the group attestation tick |
| `status` | current status |
| `releasedAt`, `receivedAt` | donor confirms release; group confirms receipt (release must come first, R5.1) |
| `createdAt` | when the offer was sent |
| `photoPaths` | up to 5 photo paths from `offer_photo`; filled by the DAO |
| `groupName` | **display only**, from a join |
| `handoffNumber()` | `HR-<year>-<id>`; the year is `receivedAt` if completed, else `createdAt` |

## SafetyNotice
A concern raised on an accepted or completed offer.

| Member | Meaning |
|---|---|
| `Status` enum | OPEN, DISMISSED, ACCOUNT_SUSPENDED |
| `id` | notice id, shown as "Notice #12" |
| `offerId`, `raisedBy` | the offer concerned and the account that raised it. The flagged account is the other side of the offer |
| `concern` | what the user wrote |
| `status` | current status |
| `adminNote` | required when the admin resolves it |
| `resolvedBy`, `resolvedAt`, `raisedAt` | audit fields |
| `raisedByName`, `flaggedAccountName` | **display only**, from joins |

---

## Not modeled
`status_change` and `email_log` have no model class, as in the spec's tree. `LogDao` writes them from plain arguments.

## Known quirks
- `Account.OrgType` has no `label()`, so the Profile row "Type" shows the lowercase DB value (for example `bakery`). Add a label if the UI should show "Bakery".
- `requiredDocuments()` models "one of these" as a set inside a list. A service checks each slot against the registration's documents in force.
- The no-arg constructor plus setters allows partially filled objects. DAOs are expected to fill every column.

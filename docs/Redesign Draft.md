# ANIPLATO Redesign Draft (running doc)

Status: brainstorming output. `Project Spec.md` is unchanged. Everything not listed here follows the old model in the spec. Changes are finalized in this doc first, then applied to the spec, then to the code and SQL. Where the two disagree, this draft is the proposed change.

Note for the spec revision: rule IDs (R1.1, A2 and so on) are dropped in this draft and will be dropped from the spec when it is revised. Rules are plain bullets for now.

## 1. Core

ANIPLATO is a matchmaker with receipts: a listing or request, a commitment, a physical exchange, and proof that it happened. Verification exists only to make those receipts believable.

## 2. Actors

There are three roles, chosen at registration:

- **Business donor** and **Casual donor** only give.
- **Food group** only receives. It needs a standing feeding activity (feeding programs, parishes, volunteer groups). One-off individual volunteers are out of scope; they can partner with an established group.
- Every registration is admin-approved. Casual donors get a lighter review.
- One role per account. An organization that wants to both donate and receive registers a second account.
- There is no organization type. The role is the only classification.

## 3. Roles

| | Business donor | Casual donor (new) | Food group |
| :-- | :-- | :-- | :-- |
| Who | Restaurants, bakeries, canteens, caterers, groceries | Anyone with a large surplus and no permits | Parishes, schools, orphanages, charities, feeding programs, volunteer groups |
| Gives or receives | Gives | Gives | Receives |
| Documents | Business Permit and Sanitary Permit | Barangay Certificate | One credential (DSWD, PCNC, SEC non-stock, DepEd/CHED/TESDA, or a diocese endorsement) plus an authorization letter |
| Extra | FDA License only if they make or repack food. DTI, SEC or CDA registration only if the permit does not show the business name clearly | None | None |
| Food categories | Raw, packaged, baked, cooked | Raw and sealed-packaged only | Not applicable |

- Both donor roles use the same screens. The role only changes the required documents and the food categories offered on the listing form.
- Business donors can post more food categories. That is their advantage over Casual donors.
- A business that can't show both permits registers as Casual and can upgrade later.
- No kg cap for Casual donors. The category limit already controls the risk, and the food group decides whether to take the amount.

## 4. Interaction flows

**Route 1: donor first**
1. The donor posts a listing, with details and photos.
2. Food groups send offers, with details and photos.
3. The donor chooses one and confirms.

**Route 2: food group first (new)**
1. The food group posts a request, with details and photos.
2. Donors offer to fulfill it, with details and photos.
3. The food group chooses one and confirms.

**Both routes then continue as follows**
1. Once both sides have confirmed, contact numbers are revealed so they can arrange the courier or physical exchange.
2. The donor confirms release.
3. The food group confirms receipt. This automatically creates a handoff record that shows in both parties' history.
4. The food group is given a deadline to submit a simple documentation report showing the event or program that was held. The donor can see it.
5. If the group can't comply, it is flagged for suspension by admin.

## 5. Food groups can no longer post listings (new)

- With more coordination, both sides can agree on the amount (kg) before committing.
- Re-posting surplus no longer makes sense. Donations happen in bulk, so leftovers after a program mean the group requested or accepted more than it needed.

## 6. Full flows (stage by stage)

Same format as the spec's section 6: Who, Needs, Does, Result, Legal basis, Rules. Stages 2 to 6 are still to be written.

### Stage 1: Register and get verified

| Component | Detail |
| :-- | :-- |
| **Who** | New Business donor, Casual donor or Food group. |
| **Needs** | Nothing. No account exists yet. |
| **Does** | Fills one shared form for all roles: name, city, contact person, contact number, email. A Casual donor enters their own name or household name, and their own name as the contact person. Picks **Donor** or **Food group**. If Donor, picks "I have a Mayor's Permit and Sanitary Permit" (Business) or "I don't have permits" (Casual). Uploads the documents for that role, each with its expiry date. Agrees to the consent text. |
| **Result** | Registration is Pending. The user gets a "Received" email. |

**Required documents by role**

| Role | Documents |
| :-- | :-- |
| Business donor | Mayor's Permit and Sanitary Permit. FDA License to Operate only if the business makes or repacks food. DTI, SEC or CDA registration only if the permit does not show the business name clearly enough for the name check. |
| Casual donor | One Barangay Certificate, with an entered expiry date. |
| Food group | One credential (DSWD, PCNC, SEC non-stock, DepEd/CHED/TESDA, or a diocese endorsement for parishes) plus an authorization letter naming the contact person. |

**Rules**
- Each document needs an expiry date. "No expiry" is allowed only where the document truly has none. Permits and the Barangay Certificate always need an explicit date, with no "No expiry" option.
- No government IDs are collected.
- Files are visible only to the applicant and the admin.
- The admin can approve only if every document is not expired and matches the name and city. For a Casual donor the review is lighter: the admin checks the Barangay Certificate against the name and city. The phone number is only entered, not verified.
- Approve: the account is created and an email sends a temporary password.
- Reject: the admin must write a reason. The reason is emailed. The applicant may register again.
- The temporary password has no time limit. Every login with it goes to Change Password and nothing else is reachable until a new one is set.
- The role is set at registration. It decides the required documents and the food categories a donor may post: Business may post raw, packaged, baked and cooked. Casual may post raw and sealed-packaged only.
- A business that cannot show both permits is not rejected. It may register as Casual, under the Casual limits. To upgrade, the donor uploads the permits and the admin reviews them. On approval the role changes to Business on the same account, and history stays.
- A food group must show a standing feeding activity (feeding program, parish pantry, school feeding, volunteer group). One-off individual volunteers are not accepted.
- One role per account. An organization that wants to both donate and receive registers a second account with a different email.
- No organization type is collected.
- A Casual donor gives no pickup location at registration. Pickup details live in each listing's handover instructions.

**Legal basis**
- Mayor's Permit and Sanitary Permit are required of every business donor. The FDA License to Operate is needed only by makers and repackers of packaged food.
- Food groups upload one credential that matches their kind of organization.
- RA 10173 (Data Privacy Act): collect only what is needed, and no government IDs. A Barangay Certificate is a local government document, not a government ID.
- Casual donors are limited to raw and sealed-packaged food because they have no Sanitary Permit. This is a platform rule, not a legal one.

Screen changes for this stage are listed in section 7.

**Data model notes (for the data model phase)**
- `role` becomes `BUSINESS_DONOR`, `CASUAL_DONOR` or `FOOD_GROUP` in both registration and account, plus `ADMIN` on account only.
- `org_type` is removed.
- Any check that currently reads "role = DONOR" needs to cover both donor roles.

---

## 7. Proposed screen changes (running)

Running list, grouped by module. Frame names are from the Figma file. Each module is added or updated as the flows are rewritten. Items are proposals until the flows are final.

**Labels used on every screen from now on:** Business donor, Casual donor, Food group. This replaces the current "Food Donor" and "Food Organization" badges.

### Registration

**Step 1: Organization** (`REGISTER — STEP 1 ORGANIZATION`)
- Role toggle becomes **Donor | Food group**. Picking Donor shows a second choice: "I have a Mayor's Permit and Sanitary Permit" (Business) or "I don't have permits" (Casual). Business is the default.
- Remove the Organization type dropdown. City takes the full row.
- The name field label and hint follow the role:
  - Business donor: "Business name", hint "As shown on your permits"
  - Casual donor: "Your name or household name", hint "As shown on your Barangay Certificate"
  - Food group: "Organization name", hint "As shown on your credential"
- The city hint follows the role: Mayor's Permit (Business), Barangay Certificate (Casual), registered address (Food group).
- Contact person stays for every role. For Casual, the hint says to enter your own name.
- Contact number and email are unchanged.

**Step 2: Documents, Business donor** (`REGISTER — STEP 2 DOCUMENTS`)
- The "Required for" tag shows only "Business donor" (drop "· Bakery").
- "Business registration (DTI, SEC or CDA)" is no longer required. It becomes optional, with the hint "Only if your permit doesn't show your business name clearly".
- Mayor's Permit and Sanitary Permit stay required. FDA License stays optional.
- The intro sentence keeps "Permits always need a date".

**Step 2: Documents, Casual donor** (new frame: `REGISTER — STEP 2 DOCUMENTS (CASUAL)`)
- The "Required for" tag shows "Casual donor".
- One item only: **Barangay Certificate** (required), with an upload and an expiry date. There is no "No expiry" tick.
- The intro sentence says "The Barangay Certificate always needs an expiry date. Don't upload government IDs."
- The consent box and the Back and Submit buttons are the same as the other steps.

**Step 2: Documents, Food group** (`REGISTER — STEP 2 DOCUMENTS (FOOD GROUP)`)
- The "Required for" tag shows only "Food group" (drop "· Charity").
- Everything else is unchanged.

**Submitted screen and emails:** no change decided yet.

### Admin: accounts and review

**Pending Accounts** (`Pending Accounts`)
- Role badges: Business donor, Casual donor, Food group.
- Remove the Type column.
- Casual to Business upgrade requests show up here with an "Upgrade" tag next to "New" and "Registered again".

**All Accounts** (`ALL ACCOUNTS`)
- Same badge change and Type column removal.
- The Documents column reads naturally for each role (for example "Barangay Certificate ends Dec 31" for Casual).

**Review Account** (`Review Account` and its Accept and Reject dialogs)
- The Role pill shows Business donor, Casual donor or Food group.
- Remove the Organization type field.
- The document list follows the role: Business shows its permits, Casual shows just the Barangay Certificate, Food group is unchanged.
- The three checks per document stay the same for every role: current and not expired, name matches, issued for the city.

### Profile

**Donor profile** (`DONOR PROFILE`)
- Remove the TYPE field. The Role field shows Business donor or Casual donor.
- The Organization hint changes from "Name, type and city" to "Name and city".
- Casual donor: Documents lists only the Barangay Certificate, and there is an **Upgrade to Business donor** action to upload the Mayor's Permit and Sanitary Permit, plus FDA License if applicable. The admin reviews it and the role changes on approval.
- Business donor: the "Business registration (DTI)" row shows "Not required" when none was uploaded, like the FDA row does today.

**Food group profile** (`FOOD GROUP PROFILE`)
- Remove the TYPE field and change the hint to "Name and city".

### Listings (donor)

**New Listing** (`DONOR NEW LISTING`)
- The Category choices depend on the role:
  - Business donor: Raw, Cooked, Baked, Packaged.
  - Casual donor: Raw and Packaged only, with Cooked and Baked hidden. The Packaged label reads "Sealed packaged" for Casual.
- The category hint adds, for Casual: "Casual donors can post raw and sealed-packaged food only."
- Everything else on the form is unchanged for now. Photos and handover details wait for Stage 2.

### Food group navigation and screens

- Remove **My Listings** and **New Listing** from the food group sidebar. Food groups can no longer post listings.
- Delete the frames `FOOD GROUP MY LISTINGS (RE-POSTS)` and `FOOD GROUP INCOMING OFFERS (RE-POST)`.
- Route 2 needs replacement screens for requesting food. They are defined once Stage 2 is written.

### Waiting on later stages

These screens will change but can't be written until their stage is final:
- Donor and Food group dashboards (requests, report deadline, flags)
- Donor New Listing details (photos, handover)
- Food group Browse Listings and Make Offer (photos)
- New: Food group request form and a donor's offer form (Route 2)
- Donor Incoming Offers and the food group's choose-and-confirm view
- Handoff Record, then the new report submission screen
- History
- Safety Notices and suspension flows (report non-compliance flag)

---

## 8. Open points

- Whether Casual donors also need a "new donor" label until their first completed exchange.
- Photo requirements at each step, including donor photos at handoff.
- The report deadline and what the report must contain.
- Whether flagged groups are suspended automatically or only after admin review.
- How a request is stored: a separate object from a listing, or the same object with a direction field.
- Spec sections affected when formalized: users, registration documents and rules, listing and request flows, handoff, new report stage, the data model and the ER diagram.

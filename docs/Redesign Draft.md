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

**Registration screens (changes)**
- The first screen's role toggle is **Donor | Food group**. Choosing Donor shows the Business or Casual choice.
- The organization type dropdown is removed for every role.
- The form fields are otherwise the same for all roles.
- On the documents screen, the "Required for" tag shows only the role: Business donor, Casual donor or Food group.
- The Casual document list is just the Barangay Certificate with a required expiry date.

**Data model notes (for the data model phase)**
- `role` becomes `BUSINESS_DONOR`, `CASUAL_DONOR` or `FOOD_GROUP` in both registration and account, plus `ADMIN` on account only.
- `org_type` is removed.
- Any check that currently reads "role = DONOR" needs to cover both donor roles.

---

## 7. Open points

- Whether Casual donors also need a "new donor" label until their first completed exchange.
- Photo requirements at each step, including donor photos at handoff.
- The report deadline and what the report must contain.
- Whether flagged groups are suspended automatically or only after admin review.
- How a request is stored: a separate object from a listing, or the same object with a direction field.
- Spec sections affected when formalized: users, registration documents and rules, listing and request flows, handoff, new report stage, the data model and the ER diagram.

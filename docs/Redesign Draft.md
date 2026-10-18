# ANIPLATO Redesign Draft (running doc)

Status: brainstorming output. `Project Spec.md` is unchanged. Everything not listed here follows the old model in the spec. Changes are finalized in this doc first, then applied to the spec, then to the code and SQL. Where the two disagree, this draft is the proposed change.

## 1. Core

ANIPLATO is a matchmaker with receipts: a listing or request, a commitment, a physical exchange, and proof that it happened. Verification exists only to make those receipts believable.

## 2. Actors

- **Donors** only give. They are either Business or Casual.
- **Food groups** only receive. They need a standing feeding activity (feeding programs, parishes, volunteer groups). One-off individual volunteers are out of scope; they can partner with an established group.
- Every registration is admin-approved. Casual donors get a lighter review.
- One role per account. An organization that wants to both donate and receive registers a second account.

## 3. Donor lanes

| | Business donor | Casual donor (new) |
| :-- | :-- | :-- |
| Who | Restaurants, bakeries, canteens, caterers, groceries | Anyone with a large surplus and no permits |
| Documents | Business Permit and Sanitary Permit, plus business details | Barangay Certificate and verifiable contact details |
| Extra | FDA License only if they make or repack food | None |
| Food categories | Raw, packaged, baked, cooked | Raw and sealed-packaged only |

- Business donors have more categories to post. That is the advantage of the Business lane.
- A business that can't show both permits registers as Casual and can upgrade later.

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

Same format as the spec's section 6: Who, Needs, Does, Result, Legal basis, Rules. Rule IDs keep the spec's numbers where the rule is unchanged. Changed or new rules are marked **(changed)** or **(new)**. Stages 2 to 6 are still to be written.

### Stage 1: Register and get verified

| Component | Detail |
| :-- | :-- |
| **Who** | New Business donor, Casual donor or food group. |
| **Needs** | Nothing. No account exists yet. |
| **Does** | Picks a role: Business donor, Casual donor or Food group. Fills in details, contact person and number, and uploads each required document with its expiry date. Agrees to the consent text. |
| **Result** | Registration is Pending. The user gets a "Received" email. |

**Required documents by role**

| Role | Documents | Other required details |
| :-- | :-- | :-- |
| Business donor | Mayor's Permit and Sanitary Permit. DTI, SEC or CDA registration only if the permit does not show the business name clearly enough for the name check (R1.4). FDA License to Operate only if the business makes or repacks food. | Business details: name, type, city, contact person and number, pickup location. |
| Casual donor **(new)** | One Barangay Certificate. | Verifiable contact details: reachable phone number, email, and pickup location. |
| Food group | One credential (DSWD, PCNC, SEC non-stock, DepEd/CHED/TESDA, or a diocese endorsement for parishes) plus an authorization letter naming the contact person. | Organization details and the standing feeding activity (R1.10). |

**Rules**
- **R1.1** Each document needs an expiry date. "No expiry" is allowed only where the document truly has none. Permits and the Barangay Certificate always need a date. *(changed: Barangay Certificate added)*
- **R1.2** No government IDs are collected.
- **R1.3** Files are visible only to the organization (or person) and the admin.
- **R1.4** The admin can approve only if every document is not expired and matches the name and city. For a Casual donor the review is lighter: the admin checks the Barangay Certificate against the name and city, and that the contact details look reachable. *(changed)*
- **R1.5** Approve: the account is created and an email sends a temporary password.
- **R1.6** Reject: the admin must write a reason. The reason is emailed. The applicant may register again.
- **R1.7** Temporary password has no time limit. Every login with it goes to Change Password and nothing else is reachable until a new one is set.
- **R1.8 (changed)** A donor registers in one of two lanes, Business or Casual. Both are admin-approved before they can transact. The lane decides the required documents (above) and the food categories they may post (Stage 2): Business may post raw, packaged, baked and cooked. Casual may post raw and sealed-packaged only.
- **R1.9 (changed)** A business that cannot show both permits is not rejected. It may register as Casual, under the Casual limits. A Casual donor may upgrade to Business at any time by uploading the permits, which the admin reviews under R1.4. The account keeps its history.
- **R1.10** A food group must show a standing feeding activity (feeding program, parish pantry, school feeding, volunteer group). One-off individual volunteers are not accepted.
- **R1.11 (new)** One role per account. An organization that wants to both donate and receive registers a second account with a different email.

**Legal basis (unchanged except as noted)**
- Mayor's Permit and Sanitary Permit are required of every business donor (R1.1, R1.4). The FDA License to Operate is needed only by makers and repackers of packaged food.
- Food groups upload one credential that matches their kind of organization.
- RA 10173 (Data Privacy Act): collect only what is needed, and no government IDs (R1.2, R1.3). A Barangay Certificate is a local government document, not a government ID. **(new)**
- Casual donors are limited to raw and sealed-packaged food because they have no Sanitary Permit. This is a platform rule, not a legal one.

---

## 7. Open points

- Whether Casual donors also need a "new donor" label until their first completed exchange.
- Photo requirements at each step, including donor photos at handoff.
- The report deadline and what the report must contain.
- Whether flagged groups are suspended automatically or only after admin review.
- Stage 1: how the admin "verifies" Casual contact details (reachable phone only, or a call or email check).
- Stage 1: Barangay Certificate validity. Most barangays issue them for about 6 months, so it needs an expiry date under R1.1. Confirm that is acceptable.
- Stage 1: what a Casual donor is called on screen and in the data (org name and org type don't fit an individual).
- Stage 1: can a Casual donor post before their first approved upgrade only at the limits above, or do they also get a per-listing kg cap?
- How a request is stored: a separate object from a listing, or the same object with a direction field.
- Spec sections affected when formalized: users, registration documents and rules, listing and request flows, handoff, new report stage, the data model and the ER diagram.

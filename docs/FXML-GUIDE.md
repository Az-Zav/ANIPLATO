# FXML Guide

How to build a screen in Scene Builder so the Java side can wire it up later without changing your file.

You touch the screen's `.fxml` file, `aniplato.css`, and a small, fixed part of its controller: `@FXML` fields, placeholder handlers and `TODO(service)` comments (section 10). You never need the services, models or database to finish a screen. A brand-new screen also needs one line in `Navigator` (section 11).

---

## 1. Tags

Put one of these comments directly above every element the code will touch. Searching for `PATTERN:` finds every wiring point; `TODO(service)` finds every place still waiting for real logic.

| Tag | Use it on | Example |
|---|---|---|
| `PATTERN:NAV` | A button or link that only goes to another screen | `<!-- PATTERN:NAV -> LOGIN -->` |
| `PATTERN:ACTION` | A button that will do real work (save, check, log in) | `<!-- PATTERN:ACTION handleSubmit -->` |
| `PATTERN:DYNAMIC` | Text or visibility the code will set | `<!-- PATTERN:DYNAMIC orgName -->` |
| `PATTERN:LIST` | A table or set of repeated rows the code will fill | `<!-- PATTERN:LIST listings (sample row below) -->` |
| `TODO(service)` | Java only: where a service, model or session call goes later | `// TODO(service): registrationService.submit(...)` |

The same tags appear in the controllers, so both sides of a wiring point are easy to find.

---

## 2. Buttons: navigate or act, never both

**Only goes somewhere** → add a `nav-<screen>` style class. No `onAction`, no Java.

```xml
<!-- PATTERN:NAV -> REGISTER -->
<Hyperlink text="Register" styleClass="hyperlink, nav-register"/>
```

**Will do work** → add `onAction="#handleSomething"`, and add a placeholder `handleSomething()` to the controller that only navigates until the real logic exists (section 10).

```xml
<!-- PATTERN:ACTION handleSubmit -->
<Button text="Submit registration" styleClass="primary-button" onAction="#handleSubmit"/>
```

A button never has both a `nav-*` class and an `onAction`.

### Multiple style classes need commas

```xml
styleClass="hyperlink, nav-register"     <!-- right: two classes -->
styleClass="hyperlink nav-register"      <!-- wrong: ONE class called "hyperlink nav-register" -->
```

With a space, both the CSS and the navigation silently stop working. The app prints a `WARNING` naming the file when it sees this.

### `nav-` names

The name is the screen from `Navigator.Screen`, lowercased, with `_` turned into `-`. A typo fails when the button is clicked, with a message naming the bad class.

| Screen | Class |
|---|---|
| LOGIN | `nav-login` |
| REGISTER | `nav-register` |
| REGISTER_STEP2 | `nav-register-step2` |
| REGISTER_SUBMITTED | `nav-register-submitted` |
| PROFILE | `nav-profile` |
| ADMIN_DASHBOARD | `nav-admin-dashboard` |
| PENDING_REGISTRATIONS | `nav-pending-registrations` |
| REVIEW_REGISTRATION | `nav-review-registration` |
| ACCOUNTS | `nav-accounts` |
| SAFETY_NOTICES | `nav-safety-notices` |
| REPORTS | `nav-reports` |
| DONOR_DASHBOARD | `nav-donor-dashboard` |
| FOODGROUP_DASHBOARD | `nav-foodgroup-dashboard` |
| BROWSE_LISTINGS | `nav-browse-listings` |
| MAKE_OFFER | `nav-make-offer` |
| MY_OFFERS | `nav-my-offers` |
| LISTING_FORM | `nav-listing-form` |
| MY_LISTINGS | `nav-my-listings` |
| LISTING_OFFERS | `nav-listing-offers` |
| HISTORY | `nav-history` |
| HANDOFF_RECORD | `nav-handoff-record` |

Need a screen that isn't listed? Add it to `Navigator.Screen` first (see section 11).

---

## 3. Dynamic text

**A label with an `fx:id` is data. A label without one is fixed text.**

```xml
<Label text="Registration submitted"/>                       <!-- fixed: no fx:id -->

<!-- PATTERN:DYNAMIC orgSubhead : "{orgName} · {city}" -->
<Label fx:id="orgSubheadLabel" text="Sample Bakeshop · Sample City"/>
```

In the controller you add the field and a `TODO(service)` line (section 10). Later, the Java dev turns that line into real code in `initialize()`, and the placeholder is simply overwritten:

```java
// PATTERN:DYNAMIC fills placeholders
@FXML
private void initialize() {
    orgSubheadLabel.setText(registration.getOrgName() + " · " + registration.getCity());
}
```

Rules:
- **Placeholders look fake but realistic**: `Sample Bakeshop`, `sample@organization.ph`, `00.0%`. Not real company names, not `???`.
- **Use the longest realistic value**, so you see whether the layout breaks.
- **One value per label.** Write `[kgLabel "48.0"] [Label "kg"]`, not `"Total: 48.0 kg"` in one label.
- **If a value sits inside a sentence**, give the whole sentence one `fx:id` and write the template in the tag: `<!-- PATTERN:DYNAMIC emailNotice : "We sent a confirmation to {email}." -->`
- **Numbers and dates** show their format in the placeholder: `48.0`, `94.6%`, `Oct 4, 9:14 AM`.
- **Name by what it shows, plus the type**: `orgNameLabel`, `kgListedLabel`, `statusPill`, `listingsTable`.

## 4. Dynamic visibility

When a screen shows different sections for different cases, build every section, give each its own `fx:id`, and hide all but the default:

```xml
<!-- PATTERN:DYNAMIC visibility: donorDocsBox shown for business donors (default) -->
<VBox fx:id="donorDocsBox"> ... </VBox>

<!-- PATTERN:DYNAMIC visibility: foodGroupDocsBox shown for food groups (hidden by default) -->
<VBox fx:id="foodGroupDocsBox" visible="false" managed="false"> ... </VBox>
```

`managed="false"` makes the hidden section take no space. To design a hidden section, flip both values in Scene Builder, then flip them back before you save.

## 4b. Worked example: different cards per role

**When to use this:** one screen shows different cards depending on who is using it. Example: on Registration step 2, a business donor uploads 2 documents and a food group uploads 2 *different* documents.

**The idea:** build every version in the FXML. The code only decides which one is shown. You never add or remove cards from Java for this.

### Step 1: One container per case

Wrap each set of cards in its own `VBox` and give it an `fx:id`. Name it after the case, ending in `Box`.

```xml
<!-- PATTERN:DYNAMIC visibility: donorDocsBox shown for business donors (default) -->
<VBox fx:id="donorDocsBox">
    <!-- card 1: Business registration -->
    <!-- card 2: Mayor's permit -->
</VBox>

<!-- PATTERN:DYNAMIC visibility: foodGroupDocsBox shown for food groups (hidden by default) -->
<VBox fx:id="foodGroupDocsBox" visible="false" managed="false">
    <!-- card 1: Credential -->
    <!-- card 2: Authorization letter -->
</VBox>
```

### Step 2: Pick one default, hide the rest

- The **default** case is visible. Leave it alone.
- Every other case gets `visible="false" managed="false"`.
- `visible` hides it. `managed` makes it take no space, so there's no gap.

To design a hidden box, flip both values to `true` in Scene Builder, then flip them back to `false` before you save.

### Step 3: Tag changing text inside the cards

Each card's file name is data, so it gets an `fx:id` and a `PATTERN:DYNAMIC` tag, like any dynamic label (section 3).

```xml
<!-- PATTERN:DYNAMIC businessRegFile : uploaded file name -->
<Label fx:id="businessRegFileLabel" text="business-permit.pdf"/>
```

### Step 4: Declare the controller fields

In the controller, add one `@FXML` field per container and a `TODO(service)` line saying what decides it. Don't write the logic.

```java
@FXML private VBox donorDocsBox;
@FXML private VBox foodGroupDocsBox;

// PATTERN:DYNAMIC fills placeholders
@FXML
private void initialize() {
    // TODO(service): show donorDocsBox or foodGroupDocsBox based on the role in the registration draft
}
```

### What the Java dev does later

They turn that TODO into this, and nothing in your FXML changes:

```java
boolean foodGroup = RegistrationDraft.role() == Role.FOOD_GROUP;
donorDocsBox.setVisible(!foodGroup);
donorDocsBox.setManaged(!foodGroup);
foodGroupDocsBox.setVisible(foodGroup);
foodGroupDocsBox.setManaged(foodGroup);
```

The controller only decides what to show. Rules, such as which documents are required to submit, stay in the service (A1).

### Checklist before you commit

- [ ] Every case has its own container with an `fx:id`.
- [ ] Exactly one container is visible by default.
- [ ] All hidden containers have both `visible="false"` and `managed="false"`.
- [ ] Each container has a `PATTERN:DYNAMIC visibility:` comment above it.
- [ ] The controller has a field and a `TODO(service)` line for each container.

### Use this vs. a list

| Situation | Pattern |
|---|---|
| A fixed set of cards that differs by role or case | This section: build all, hide some |
| Rows that come from data (listings, pending accounts) | Section 5: one sample row, `PATTERN:LIST` |

## 5. Lists and tables

- Design **one** sample row or card, tag the container `PATTERN:LIST`, and give the container an `fx:id`. The code removes the sample and adds one row per item.
- A `TableView` gets an `fx:id`, and each `TableColumn` gets its own `fx:id` (`descriptionCol`, `kgCol`).
- If a row or card repeats in more than one place, put it in its own small FXML file.
- Add an empty-state label (`emptyLabel`, "No listings yet") next to the list.

## 6. Status colors

Never color a status by hand (`textFill="#15803D"`). Add one CSS class per status to `aniplato.css` (`pill-open`, `pill-accepted`, `pill-rescued`, `pill-expired`...). The code picks the class from the status.

## 7. Layout and style

- **Screens inside the shell** (everything except login, register and the shell itself) have one root, usually a `VBox`, with **no header, sidebar or logout**. The shell provides those.
- **Style with `styleClass` only.** Put colors, fonts and borders in `aniplato.css`. No `style="-fx-..."`, and avoid `textFill` / `<font>` on new screens.
- **Keep the stylesheet line** on the root so Scene Builder previews with the real look: `@aniplato.css` for files in `view/`, `@../aniplato.css` for files in a subfolder.
- **No paths and no navigation logic** in FXML. Don't `fx:include` other screens.
- **Leave `fx:controller` as it is.** File names and folders follow the spec (`view/admin/`, `view/shared/`...).

## 8. Checking your screen

1. Preview in Scene Builder (**Preview → Show Preview in Window**). It should look finished, with sample data.
2. Every `nav-*`, every `onAction` and every code-touched `fx:id` has a tag comment above it.
3. No style class contains a space.
4. Hand over the contract below.

## 9. Contract template (hand this over with each screen)

```
<file>.fxml
  fx:id / handler        type          tag          source / notes
  orgSubheadLabel        Label         DYNAMIC      "{orgName} · {city}"
  donorDocsBox           VBox          DYNAMIC      visible for business donors
  pendingRowsBox         VBox          LIST         one row per pending registration
  #handleSubmit          Button        ACTION       submits the registration
  nav-login              Button        NAV          back to login
```

Scene Builder's **View → Show Sample Controller Skeleton** lists every `fx:id` and handler for you; paste that alongside the table.

## 10. Writing the controller

The controller is the other half of each tag. You write only the wiring; the real logic arrives later at the `TODO(service)` lines.

### What each tag needs in the controller

| In the FXML | In the controller | Required? |
|---|---|---|
| `PATTERN:NAV` (`nav-*` class) | Nothing | — |
| `PATTERN:ACTION` (`onAction="#handleX"`) | A placeholder method `handleX()` | **Yes.** Without it the screen fails to load. |
| `PATTERN:DYNAMIC` (`fx:id`) | An `@FXML` field and a `TODO(service)` line in `initialize()` | Recommended. It records what fills the placeholder. |
| `PATTERN:LIST` (`fx:id` on the container) | An `@FXML` field and a `TODO(service)` line in `initialize()` | Recommended. |

### Template

```java
package com.aniplato.controller.shared;

import com.aniplato.util.Navigator;
import com.aniplato.util.Navigator.Screen;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class ChangePasswordController {

    // 1. One @FXML field per fx:id the code will touch. The name matches the fx:id exactly.
    @FXML private Label accountNameLabel;
    @FXML private VBox historyRowsBox;

    // 2. DYNAMIC and LIST: say what fills each placeholder, as TODO(service) comments.
    // PATTERN:DYNAMIC fills placeholders
    @FXML
    private void initialize() {
        // TODO(service): accountNameLabel.setText(account.getOrgName())

        // PATTERN:LIST fills rows
        // TODO(service): one row in historyRowsBox per password change
    }

    // 3. ACTION: one placeholder per onAction. Describe the future logic, then navigate (or do nothing).
    // PATTERN:ACTION placeholder: navigates only
    @FXML
    private void handleSave() {
        // TODO(service): authenticationService.changePassword(...) -- R1.7
        Navigator.show(Screen.PROFILE);
    }

    // PATTERN:ACTION placeholder: does nothing yet
    @FXML
    private void handleShowRules() {
        // TODO: show the password rules
    }
}
```

### Comment formats

| Comment | Where | What it says |
|---|---|---|
| `// PATTERN:DYNAMIC fills placeholders` | Above `initialize()` (or its block) | This method writes the tagged labels |
| `// PATTERN:LIST fills rows` | Above the rows block in `initialize()` | This block fills the tagged list |
| `// PATTERN:ACTION placeholder: navigates only` | Above a handler that only navigates for now | The future logic goes above the `Navigator.show` line |
| `// PATTERN:ACTION placeholder: does nothing yet` | Above an empty handler | No behavior yet |
| `// TODO(service): <call or setText> -- <rule IDs>` | Inside the method, where the real line will go | The exact call or assignment planned, plus spec rule IDs (R1.7, A8...) |
| `// TODO: <what>` | For work that isn't a service call (UI behavior, missing file) | A plain note |

Write `TODO(service)` lines as the code they will become (`label.setText(...)`, `xService.method(...)`), so the Java dev can uncomment and adjust instead of guessing.

### Allowed and not allowed

**Allowed:**
- `@FXML` fields
- placeholder handlers
- `Navigator.show(Screen.X)`
- `TODO` comments
- pure UI state, such as moving a highlight class or showing and hiding a section by a checkbox

**Not allowed (left for the Java devs):**
- imports from `service`, `dao` or `model`, or of `Session`
- `FXMLLoader`, `Stage`, `getResource` or any view path. Use `Navigator`.
- rules or validation, such as checking that a field is empty or a date is in the future. Write a `TODO(service)` instead, since rules live in services (A1).
- hardcoded fake data in `setText(...)`. Sample values belong in the FXML `text`, not in Java.
- `System.out` / `printStackTrace` "success" messages

### How a placeholder becomes real later

The tags and the `Navigator.show` line stay; only the `TODO` lines change.

```java
// before (placeholder)
// PATTERN:ACTION placeholder: navigates only
@FXML
private void handleSave() {
    // TODO(service): authenticationService.changePassword(...) -- R1.7
    Navigator.show(Screen.PROFILE);
}

// after (Java dev)
// PATTERN:ACTION
@FXML
private void handleSave() {
    try {
        authenticationService.changePassword(newPasswordField.getText());   // R1.7
        Navigator.show(Screen.PROFILE);
    } catch (RuleViolationException e) {
        DialogController.showError(e.getMessage());                         // A8
    }
}
```

## 11. Adding a new screen

Every screen needs three things: an FXML file, a controller class, and one line in `Navigator.Screen`. The example adds a "change password" screen inside the shell.

**Step 1: the FXML file.** Put it in the folder for its role (`view/admin/`, `view/donor/`, `view/foodgroup/`, or `view/shared/` if more than one role uses it). Use lowercase names with hyphens.

```xml
<!-- src/main/java/com/aniplato/view/shared/change-password.fxml -->
<VBox xmlns="http://javafx.com/javafx/21" xmlns:fx="http://javafx.com/fxml/1"
      fx:controller="com.aniplato.controller.shared.ChangePasswordController"
      spacing="20.0" styleClass="dashboard-bg">
    <stylesheets>
        <URL value="@../aniplato.css" />
    </stylesheets>
    ...
</VBox>
```

**Step 2: the controller.** Same package path as the folder, named after the screen plus `Controller`. Write it with the section 10 template: an `@FXML` field per tagged `fx:id`, `TODO(service)` lines, and a placeholder method for each `onAction` (without those, the screen won't load).

```java
// src/main/java/com/aniplato/controller/shared/ChangePasswordController.java
package com.aniplato.controller.shared;

public class ChangePasswordController {
}
```

**Step 3: one line in `Navigator.Screen`.** Open `util/Navigator.java` and add an entry in the matching group:

```java
// Screens loaded into the shell's content area
...
HANDOFF_RECORD("shared/handoff-record.fxml", Kind.IN_SHELL),
CHANGE_PASSWORD("shared/change-password.fxml", Kind.IN_SHELL);   // new
```

- **Name:** upper case with underscores. It decides the `nav-` class: `CHANGE_PASSWORD` gives `nav-change-password`.
- **Path:** relative to `view/`, with the exact file name.
- **Kind:**
  - `IN_SHELL` for anything a logged-in user sees. It loads inside the shell's content area, and the shell supplies the header and sidebar.
  - `WINDOW` for screens that replace the whole window (login, register, and the shell itself). A `WINDOW` screen draws its own full layout.

**Step 4: link to it.** Nothing else is needed:

```xml
<!-- PATTERN:NAV -> CHANGE_PASSWORD -->
<Button text="Change password" styleClass="secondary-button, nav-change-password"/>
```

or from a controller, after some logic:

```java
Navigator.show(Screen.CHANGE_PASSWORD);
```

**Step 5: update the docs.** Add the new row to the `nav-` table in section 2 and the file to the spec's project tree (7.2).

**Checklist:**

| Check | If it's wrong |
|---|---|
| The `Screen` path matches the file name and folder exactly | `Missing view: ...` when you open the screen |
| `fx:controller` names a class that exists | `LoadException` when you open the screen |
| Every `onAction="#x"` has a method `x` in the controller | `LoadException` when you open the screen |
| The `nav-` class matches the enum name | `Unknown nav target: nav-...` when you click |
| `mvn clean compile` passes after editing `Navigator` | The app won't start |

Don't load FXML anywhere else. `Navigator` is the only class that knows view paths.

---

## Worked example: register-submitted.fxml

FXML (what you write):

```xml
<!-- PATTERN:DYNAMIC orgSubhead : "{orgName} · {city}" -->
<Label fx:id="orgSubheadLabel" text="Sample Bakeshop · Sample City"/>

<!-- PATTERN:NAV -> LOGIN -->
<Button text="Back to login" styleClass="primary-button, nav-login"/>
```

Controller (what you write too; no handler for the nav button):

```java
public class RegisterSubmittedController {

    @FXML private Label orgSubheadLabel;

    // PATTERN:DYNAMIC fills placeholders
    @FXML
    private void initialize() {
        // TODO(service): orgSubheadLabel.setText(orgName + " · " + city)
    }
}
```

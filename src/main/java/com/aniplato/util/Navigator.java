package com.aniplato.util;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.net.URL;

/**
 * Switches screens. The only class that knows view paths.
 *
 * Two ways to navigate (see docs/FXML-GUIDE.md):
 *  - Navigation-only buttons get a "nav-<screen>" style class in FXML; no controller code.
 *  - Buttons that do logic call Navigator.show(Screen.X) from their controller handler.
 */
public final class Navigator {

    public enum Kind { WINDOW, IN_SHELL }

    public enum Screen {
        // Full-window screens (replace the whole scene)
        LOGIN("login.fxml", Kind.WINDOW),
        REGISTER("register.fxml", Kind.WINDOW),
        REGISTER_STEP2("register-step2.fxml", Kind.WINDOW),
        REGISTER_SUBMITTED("register-submitted.fxml", Kind.WINDOW),
        SHELL("shell.fxml", Kind.WINDOW),

        // Screens loaded into the shell's content area
        PROFILE("profile.fxml", Kind.IN_SHELL),
        ADMIN_DASHBOARD("admin/dashboard.fxml", Kind.IN_SHELL),
        PENDING_REGISTRATIONS("admin/pending-registrations.fxml", Kind.IN_SHELL),
        REVIEW_REGISTRATION("admin/review-registration.fxml", Kind.IN_SHELL),
        ACCOUNTS("admin/accounts.fxml", Kind.IN_SHELL),
        SAFETY_NOTICES("admin/safety-notices.fxml", Kind.IN_SHELL),
        REPORTS("admin/reports.fxml", Kind.IN_SHELL),
        DONOR_DASHBOARD("donor/dashboard.fxml", Kind.IN_SHELL),
        FOODGROUP_DASHBOARD("foodgroup/dashboard.fxml", Kind.IN_SHELL),
        BROWSE_LISTINGS("foodgroup/browse-listings.fxml", Kind.IN_SHELL),
        MAKE_OFFER("foodgroup/make-offer.fxml", Kind.IN_SHELL),
        MY_OFFERS("foodgroup/my-offers.fxml", Kind.IN_SHELL),
        LISTING_FORM("shared/listing-form.fxml", Kind.IN_SHELL),
        MY_LISTINGS("shared/my-listings.fxml", Kind.IN_SHELL),
        LISTING_OFFERS("shared/listing-offers.fxml", Kind.IN_SHELL),
        HISTORY("shared/history.fxml", Kind.IN_SHELL),
        HANDOFF_RECORD("shared/handoff-record.fxml", Kind.IN_SHELL);

        private final String path;
        private final Kind kind;

        Screen(String path, Kind kind) {
            this.path = path;
            this.kind = kind;
        }

        /** The style-class suffix FXML uses: REGISTER_STEP2 -> "register-step2" (class "nav-register-step2"). */
        public String key() {
            return name().toLowerCase().replace('_', '-');
        }

        static Screen fromKey(String key) {
            for (Screen s : values()) {
                if (s.key().equals(key)) return s;
            }
            throw new IllegalStateException("Unknown nav target: nav-" + key + " (see Navigator.Screen)");
        }
    }

    private static final String VIEW_ROOT = "/com/aniplato/view/";
    private static final String NAV_PREFIX = "nav-";

    private static Scene scene;
    private static StackPane content;

    private Navigator() {}

    /** Called once from Main. Also wires every "nav-*" button in the app. */
    public static void init(Scene s) {
        scene = s;
        // PATTERN:NAV one handler for the whole scene; ActionEvents bubble up from any button
        scene.addEventHandler(ActionEvent.ACTION, e -> {
            if (e.getTarget() instanceof Node node) {
                for (String cls : node.getStyleClass()) {
                    if (cls.startsWith(NAV_PREFIX)) {
                        e.consume();
                        show(Screen.fromKey(cls.substring(NAV_PREFIX.length())));
                        return;
                    }
                }
            }
        });
    }

    /** Called by ShellController so in-shell screens know where to go. */
    public static void setContent(StackPane area) {
        content = area;
    }

    public static void show(Screen screen) {
        // TODO(service): MaintenanceService.runMaintenance() runs on every screen load (6.6, A4)
        if (screen.kind == Kind.WINDOW) {
            content = null;                       // the shell sets it again when it loads
            scene.setRoot(load(screen));
        } else {
            if (content == null) show(Screen.SHELL);
            content.getChildren().setAll(load(screen));
        }
    }

    private static Parent load(Screen screen) {
        URL url = Navigator.class.getResource(VIEW_ROOT + screen.path);
        if (url == null) throw new IllegalStateException("Missing view: " + VIEW_ROOT + screen.path);
        try {
            Parent root = FXMLLoader.load(url);
            warnOnSpacedStyleClasses(root, screen);
            return root;
        } catch (IOException e) {
            throw new IllegalStateException("Cannot load view: " + screen.path, e);
        }
    }

    /** FXML separates style classes with commas; "a b" silently becomes one class named "a b". */
    private static void warnOnSpacedStyleClasses(Parent root, Screen screen) {
        for (Node n : root.lookupAll("*")) {
            for (String cls : n.getStyleClass()) {
                if (cls.contains(" ")) {
                    System.err.println("WARNING " + screen.path + ": styleClass \"" + cls
                            + "\" has a space; use commas (\"" + cls.trim().replaceAll(" +", ", ") + "\")");
                }
            }
        }
    }
}

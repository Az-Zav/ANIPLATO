package com.aniplato.util;

import com.aniplato.support.Fx;
import javafx.geometry.Insets;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Region;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The space kept under a page for the floating assistant button: raised to the minimum, never lowered. */
class NavigatorRoomTest {

    @BeforeAll
    static void startToolkit() {
        Fx.start();
    }

    private static Insets after(Insets before) {
        return Fx.call(() -> {
            Region body = new Region();
            body.setPadding(before);
            Navigator.leaveRoomForAssistant(new ScrollPane(body));
            return body.getPadding();
        });
    }

    @Test
    void aPageWithLittleRoomIsRaisedToTheMinimumAndTheOtherSidesStay() {
        Insets now = after(new Insets(32, 24, 32, 16));
        assertEquals(Settings.ASSISTANT_CLEARANCE, now.getBottom());
        assertEquals(32, now.getTop());
        assertEquals(24, now.getRight());
        assertEquals(16, now.getLeft());
    }

    @Test
    void aPageThatAlreadyHasMoreRoomKeepsIt() {
        assertEquals(200, after(new Insets(10, 10, 200, 10)).getBottom());
    }

    @Test
    void somethingThatIsNotAScrollingPageIsLeftAlone() {
        Fx.run(() -> Navigator.leaveRoomForAssistant(new Region()));   // no exception
    }
}

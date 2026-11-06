package com.aniplato.controller;

import com.aniplato.support.Fx;
import javafx.scene.Parent;
import javafx.scene.control.TextField;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Registration step 1: the contact number field takes the number the way people write it, and keeps letters out. */
class RegisterScreenTest extends ScreenTestBase {

    private static TextField contactNumber(Parent screen) {
        return all(screen, TextField.class).stream().filter(t -> "e.g., 0917 123 4567".equals(t.getPromptText()))
                .findFirst().orElseThrow(() -> new AssertionError("no contact number field"));
    }

    @Test
    void theFormsOwnExampleNumberCanBeTyped() {
        TextField field = contactNumber(open("register.fxml"));
        Fx.run(() -> field.setText("0917 123 4567"));
        assertEquals("0917 123 4567", field.getText(), "the example in the prompt is accepted as typed");
        Fx.run(() -> field.setText("0917-123-4567"));
        assertEquals("0917-123-4567", field.getText());
    }

    @Test
    void lettersAndAnEndlessLineAreKeptOut() {
        TextField field = contactNumber(open("register.fxml"));
        Fx.run(() -> field.setText("0917"));
        Fx.run(() -> field.setText("0917abc"));
        assertEquals("0917", field.getText(), "a letter is refused, the text stays as it was");
        Fx.run(() -> field.setText("0917 123 4567 89012"));
        assertEquals("0917", field.getText(), "more than the longest written form is refused");
    }
}

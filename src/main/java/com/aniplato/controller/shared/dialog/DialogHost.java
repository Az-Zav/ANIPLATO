package com.aniplato.controller.shared.dialog;

import javafx.scene.Node;

/** What a dialog asks of the overlay it sits in. DialogController is the only implementation. */
public interface DialogHost {

    /** Opens this dialog card and closes any other. */
    void show(Node card);

    /** Closes every dialog and hides the overlay. */
    void close();

    /** The Release and Receipt dialogs link to the Flag Safety Concern dialog. */
    void showFlagSafetyConcern();
}

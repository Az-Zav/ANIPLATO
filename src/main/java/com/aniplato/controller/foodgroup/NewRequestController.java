package com.aniplato.controller.foodgroup;

import com.aniplato.controller.shared.DialogController;
import com.aniplato.controller.DateTimeField;
import com.aniplato.controller.FormInput;
import com.aniplato.controller.FormSupport;
import com.aniplato.controller.PhotoPicker;
import com.aniplato.model.FoodDetail;
import com.aniplato.service.PostService;
import com.aniplato.util.Navigator;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/** The food group's New Request form (R2.2). Reads the fields and hands them to PostService, which owns every rule. */
public class NewRequestController {
    @FXML private DialogController dialogController;


    private final PostService postService = new PostService();
    private final Map<FoodDetail.Category, CheckBox> categoryChecks = new EnumMap<>(FoodDetail.Category.class);

    @FXML private TextField foodNeededField;
    @FXML private TextField eventNameField;
    @FXML private DatePicker eventDatePicker;
    @FXML private TextField beneficiariesField;
    @FXML private TextField headcountField;
    @FXML private TextField placeField;
    @FXML private FlowPane categoryBox;
    @FXML private TextField kgField;
    @FXML private DateTimeField neededByField;
    @FXML private PhotoPicker photoPicker;
    @FXML private TextArea messageArea;
    @FXML private CheckBox attestationCheck;

    @FXML
    private void initialize() {
        for (FoodDetail.Category category : FoodDetail.Category.values()) {
            CheckBox box = new CheckBox(category.label());
            box.getStyleClass().add("chip-check");
            categoryChecks.put(category, box);
            categoryBox.getChildren().add(box);
        }
    }

    // PATTERN:ACTION posts the request, then opens My Requests
    @FXML
    private void handlePost() {
        FormInput input = new FormInput();
        boolean saved = FormSupport.attempt(() -> {
            Set<FoodDetail.Category> accepted = EnumSet.noneOf(FoodDetail.Category.class);
            categoryChecks.forEach((category, box) -> {
                if (box.isSelected()) accepted.add(category);
            });
            postService.createRequest(new PostService.RequestForm(foodNeededField.getText(), eventNameField.getText(), eventDatePicker.getValue(),
                    beneficiariesField.getText(), input.whole(headcountField.getText(), "how many people", FormInput.HEADCOUNT, 0),
                    placeField.getText(), input.when(neededByField, "the needed-by time", FormInput.NEEDED_BY), messageArea.getText(), accepted,
                    input.number(kgField.getText(), "the weight", FormInput.WEIGHT), attestationCheck.isSelected(), photoPicker.files()));
        }, input);
        if (saved) dialogController.showPostedMode("Request posted", "Donors can see it on their board now.",
                "View my requests", () -> Navigator.show(Navigator.Screen.MY_REQUESTS),
                "Post another", () -> Navigator.show(Navigator.Screen.NEW_REQUEST));
    }

    // PATTERN:ACTION back to My Requests
    @FXML
    private void handleCancel() {
        Navigator.show(Navigator.Screen.MY_REQUESTS);
    }
}

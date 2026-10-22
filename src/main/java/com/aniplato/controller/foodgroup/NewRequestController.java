package com.aniplato.controller.foodgroup;

import com.aniplato.controller.DateTimeField;
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

    private final PostService postService = new PostService();
    private final Map<FoodDetail.Category, CheckBox> categoryChecks = new EnumMap<>(FoodDetail.Category.class);

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
            categoryChecks.put(category, box);
            categoryBox.getChildren().add(box);
        }
    }

    // PATTERN:ACTION posts the request, then opens My Requests
    @FXML
    private void handlePost() {
        boolean saved = FormSupport.attempt(() -> {
            Set<FoodDetail.Category> accepted = EnumSet.noneOf(FoodDetail.Category.class);
            categoryChecks.forEach((category, box) -> {
                if (box.isSelected()) accepted.add(category);
            });
            postService.createRequest(new PostService.RequestForm(eventNameField.getText(), eventDatePicker.getValue(),
                    beneficiariesField.getText(), FormSupport.wholeNumber(headcountField.getText(), "how many people"),
                    placeField.getText(), neededByField.value("the needed-by time"), messageArea.getText(), accepted,
                    FormSupport.number(kgField.getText(), "the weight"), attestationCheck.isSelected(), photoPicker.files()));
        });
        if (saved) Navigator.show(Navigator.Screen.MY_REQUESTS);
    }

    // PATTERN:ACTION back to My Requests
    @FXML
    private void handleCancel() {
        Navigator.show(Navigator.Screen.MY_REQUESTS);
    }
}

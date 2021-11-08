package sample.editDevTypesForm;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import sample.models.mDevType;


public class settingDevTypesFormController {
    private boolean dialogResult = false;
    private mDevType model;

    @FXML
    private Label labelDevInfo;
    @FXML
    private TextField textBoxChannel;
    @FXML
    private Button buttonCancel;
    @FXML
    private Button buttonOk;

    public boolean isDialogResult() {
        return dialogResult;
    }

    public mDevType getModel() {
        return model;
    }

    public void initData(mDevType model) {
        this.model = model;
        labelDevInfo.setText(model.getCode());
        textBoxChannel.setText(Integer.toString(model.getPrior()));
    }

    @FXML
    public void buttonOkClick() {
        if (textBoxChannel.getText().isEmpty()) {
            showError("Не заполнен приоритет.");
            return;
        }

        //Не число.
        int a = 0;
        try {
            a = Integer.parseInt(textBoxChannel.getText());
        } catch (Exception ex) {
            showError("Введите число в приоритет.");
            return;
        }


        if ((a < 0) || (a == 0)) {
            showError("Приоритет не может быть отрицательным числом или нулем.");
            return;
        }


        model.setPrior(a); //Передаю модели приоритет.

        dialogResult = true;
        //Получаем адрес сцены.
        Stage stage = (Stage) buttonOk.getScene().getWindow();
        //Закрываем.
        stage.close();

    }

    @FXML
    public void buttonCancelClick() {
        dialogResult = false;
        //Получаем адрес сцены.
        Stage stage = (Stage) buttonCancel.getScene().getWindow();
        //Закрываем.
        stage.close();
    }

    private void showError(String text) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Ошибка");
        alert.setHeaderText("");
        alert.setContentText(text);
        alert.showAndWait();
    }

}

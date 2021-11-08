package sample.editDevForm;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import sample.mComboList;
import sample.models.mDeviceInfo;


public class settingDevFormController {
    private boolean dialogResult = false;
    private mDeviceInfo model;

    @FXML
    private Label labelDevInfo;
    @FXML
    private TextField textBoxChannel;
    @FXML
    private Button buttonCancel;
    @FXML
    private Button buttonOk;
    @FXML
    private ComboBox comboBoxWork_flg;
    @FXML
    private TextField textBox_msg_max;
    @FXML
    private TextField textBoxName;


    public boolean isDialogResult() {
        return dialogResult;
    }

    public mDeviceInfo getModel() {
        return model;
    }

    public void initData(mDeviceInfo dev) {
        this.model = dev;
        labelDevInfo.setText(dev.getDeviceInfo() + " ,uid=" + dev.getUcode());
        textBoxChannel.setText(Integer.toString(dev.getChannel()));

        if (model.getWork_flag() == 0) {
            comboBoxWork_flg.getSelectionModel().select("Отключено");
        } else {
            comboBoxWork_flg.getSelectionModel().select("Включено");
        }

        textBox_msg_max.setText(Integer.toString(model.getMsg_max()));
        textBoxName.setText(model.getName());

    }


    //Метод вызывается когда JavaFX начнет работать.
    @FXML
    public void initialize() {

        //Режимы кодировки текста сообщения.
        ObservableList<mComboWork_flg> data1 =
                FXCollections.observableArrayList(
                        new mComboWork_flg("Включено", 1),
                        new mComboWork_flg("Отключено", 0));

        comboBoxWork_flg.setItems(data1);
        comboBoxWork_flg.getSelectionModel().selectFirst();

    }


    @FXML
    public void buttonOkClick() {
        if (textBoxChannel.getText().isEmpty()) {
            showError("Не заполнен номер канала.");
            return;
        }

        if (textBox_msg_max.getText().isEmpty()) {
            showError("Не заполнено поле-количество одновременно отпр. сообщений.");
            return;
        }

        //Не число.
        int channel = 0;
        try {
            channel = Integer.parseInt(textBoxChannel.getText());
        } catch (Exception ex) {
            showError("Введите число в номер канала.");
            return;
        }


        if ((channel < 0) || (channel == 0)) {
            showError("Номер канала не может быть отрицательным числом или нулем.");
            return;
        }


        //msg_max
        int msg_max = 0;
        try {
            msg_max = Integer.parseInt(textBox_msg_max.getText());
        } catch (Exception ex) {
            showError("Количество одновременно отпр. сообщений должно быть числом");
            return;
        }

        if ((msg_max < 0) || (msg_max == 0)) {
            showError("Количество одновременно отпр. сообщений  не может быть отрицательным числом или нулем.");
            return;
        }

        String val= comboBoxWork_flg.getValue().toString();
        if(val.equals("Отключено")) model.setWork_flag(0);
        else model.setWork_flag(1);

        model.setChannel(channel);
        model.setMsg_max(msg_max);
        model.setName(textBoxName.getText());

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

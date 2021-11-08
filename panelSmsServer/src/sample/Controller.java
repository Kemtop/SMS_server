package sample;

import SmsClient.SmsClient;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;

import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import sample.editDevForm.settingDevFormController;
import sample.editDevTypesForm.settingDevTypesFormController;
import sample.models.*;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class Controller {


    @FXML
    private TextField textBoxPhoneNumber;
    @FXML
    private TextField textBoxChannel;
    @FXML
    private TextArea textAriaMessage;
    @FXML
    private ComboBox comboBoxHosts;
    @FXML
    private TableView<mDeviceInfo> tableViewDeviceInfo;
    @FXML
    private Label labelDetailInfo;
    @FXML
    private Label labelQueue;
    @FXML
    private Label labelSendTimeModem;
    @FXML
    private Label labelSendTimeApi;
    @FXML
    private Label labelSendTimeGsm;
    @FXML
    private TextField textBoxQ;
    @FXML
    private TextField textBoxtimeOutModem;
    @FXML
    private TextField textBoxtimeOutApi;
    @FXML
    private TextField textBoxtimeOutGsm;
    @FXML
    private TextField textBoxVrEnable;
    @FXML
    private TextField textBoxThreadCnt;

    @FXML
    private TableView<mDevType> tableViewDeviceType;

    //Объект для взаимодействия с сервером.
    private SmsClient sms = null;

    /**
     * Задает объект для взаимодействия с сервером.
     *
     * @param sms
     */
    public void setSms(SmsClient sms) {
        this.sms = sms;
    }

    //Метод вызывается когда JavaFX начнет работать.
    @FXML
    public void initialize() {
        //Чтение конфига.
        mConfig cfg = readConfig();
        if (cfg == null) //Возникли проблемы при чтении.
        {
            Platform.exit(); //Закрытие формы.
            return;
        }


        textBoxPhoneNumber.setText(cfg.getDefaultPhone());
        textBoxChannel.setText("1");
        textAriaMessage.setText(cfg.getDefaultText());

        //Определяем хост.
        String hostname;
        try {
            InetAddress addr;
            addr = InetAddress.getLocalHost();
            hostname = addr.getHostName();
        } catch (UnknownHostException ex) {
            System.out.println("Hostname can not be resolved");
            showError("Исключение при попытки определить хост" + ex.getMessage());
            Platform.exit(); //Закрытие формы.
            return;
        }

        setComboHost(cfg, hostname); //Заполнение выпадающего списка.


/*
        try
        {
            XmlMapper xmlMapper = new XmlMapper();
            xmlMapper.enable(SerializationFeature.INDENT_OUTPUT); //Добавление переносов после тегов.
              xmlMapper.configure(ToXmlGenerator.Feature.WRITE_XML_DECLARATION, true);
            xmlMapper.writeValue(new File("config.xml"), cfg);
        }
        catch (Exception e)
        {

        }
        */


    }


    /**
     * Задание параметров комбо боксу хостов.
     *
     * @param cfg
     */
    private void setComboHost(mConfig cfg, String hostname) {
        //Заполнение выпадающего списка.
        ObservableList<mComboList> data1 =
                FXCollections.observableArrayList();

        //Установка в комбо бокс первым хоста с параметром localhost =1.
        //Программа запущена на хосте на котором работает сервер с виртуальной машиной. Ставим первым его адрес, для удобства отладки.
        if (cfg.getLocalhostName().equals(hostname)) {
            //Первым хост виртуального сервера с параметром localhost =1.
            for (mHosts h : cfg.getHosts()) {
                if (h.getLocalhost() == 1) {
                    mComboList L = new mComboList(h.getName(), h.getValue());
                    data1.add(L);
                    break;
                }
            }

            //Все остальные
            for (mHosts h : cfg.getHosts()) {
                if (h.getLocalhost() != 1) {
                    mComboList L = new mComboList(h.getName(), h.getValue());
                    data1.add(L);
                }
            }

        } else //Не на заданном хосте.
        {
            for (mHosts h : cfg.getHosts()) {
                mComboList L = new mComboList(h.getName(), h.getValue());
                data1.add(L);
            }
        }

        comboBoxHosts.setItems(data1);
        comboBoxHosts.getSelectionModel().selectFirst();

    }


    /**
     * Читает конфигурационный файл.
     *
     * @return
     */
    private mConfig readConfig() {
        File file = new File("./config.xml");
        if (!file.exists() || file.isDirectory()) {
            showError("Отсутствует конфигруационный файл.");
            return null;//Файла нет.
        }

        StringBuilder sb = new StringBuilder();
        String line;
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            br.close();
            String xml = sb.toString();

            if (xml.length() == 0)  //Файл пуст.
            {
                showError("Конфигруационный файл пуст.");
                return null;//Файла нет.
            }


            XmlMapper xmlMapper = new XmlMapper();
            mConfig cfg = xmlMapper.readValue(xml, mConfig.class);
            return cfg;

        } catch (Exception ex) {
            showError("Исключение в методе readConfig:" + ex.getMessage());
            return null;
        }

    }


    @FXML
    public void buttonSendSmsClick() {

        //Хост на котором работает смс сервер.
        mComboList v = (mComboList) comboBoxHosts.getValue();
        String host = v.getValue();
        sms.setServerHost(host);

        if (!checkTextBox(textBoxChannel, "Номер канала:")) return; //Проверка на пустоту и отрицательные числа.

        boolean IsGood = sms.sendSms(textBoxChannel.getText(), textBoxPhoneNumber.getText(), textAriaMessage.getText());
        if (!IsGood) //Возникла ошибка в процессе.
        {
            showError("Ошибка", "", sms.getLastError());
            return;
        } else {
            showConfirm("Успешно", "", "Успешно");
        }


    }


    /**
     * Отправить пачку.
     */
    @FXML
    public void buttonSendManyClick() {
        //Хост на котором работает смс сервер.
        mComboList v = (mComboList) comboBoxHosts.getValue();
        String host = v.getValue();
        sms.setServerHost(host);

        if (!checkTextBox(textBoxChannel, "Номер канала:")) return; //Проверка на пустоту и отрицательные числа.
        if (!checkTextBox(textBoxThreadCnt, "Количество потоков:")) return;

        int cnt = 0;
        try {
            cnt = Integer.parseInt(textBoxThreadCnt.getText());
        } catch (Exception ex) {
            showError("Введите число в поле Количество потоков.");
            return;
        }


        for (int i = 0; i < cnt; i++) {

            //Запуск отдельного потока.
            Thread thread1 = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        sendInThread();
                    } catch (Exception ex) {

                    }

                }
            });

            thread1.start();
        }

        showConfirm("Информация", "", "Все потоки запущены.");

    }

    /**
     * Метод для запуска в отдельном потоке.
     */
    private void sendInThread() {
        boolean IsGood = sms.sendSms(textBoxChannel.getText(), textBoxPhoneNumber.getText(), textAriaMessage.getText());
    }

    /**
     * Событие по клику в гриде.
     */
    @FXML
    public void onTableViewDevInfoClick() {
        //showConfirm("Успешно","","Успешно");
        String name = tableViewDeviceInfo.getSelectionModel().getSelectedItem().getName();
        String server = tableViewDeviceInfo.getSelectionModel().getSelectedItem().getServer();
        String usePorts = tableViewDeviceInfo.getSelectionModel().getSelectedItem().getUsePorts();
        int work_flg = tableViewDeviceInfo.getSelectionModel().getSelectedItem().getWork_flag();

        String all = "[" + server + "] [name=" + name + "] " + " [work_flg=" + Integer.toString(work_flg) + "][" + usePorts + "]";
        labelDetailInfo.setText(all);
    }


    /**
     * Событие нажатия на кнопку "Получить сведения"
     */
    public void buttonGetServerDataClick() {
        //Хост на котором работает смс сервер.
        mComboList v = (mComboList) comboBoxHosts.getValue();
        String host = v.getValue();
        sms.setServerHost(host);


        labelDetailInfo.setText("Загрузка данных...");
        boolean IsGood = sms.getDeviceInfo();
        if (!IsGood) //Возникла ошибка в процессе.
        {

            labelDetailInfo.setText("");
            showError("Ошибка", "", sms.getLastError());
            return;
        } else {
            mDevicesInfo mData = sms.getDevicesInfo(); //Получаю модель данных.
            if (mData == null) //Проблемма с парсингом данных.
            {
                showError("Ошибка", "", sms.getLastError());
                return;
            }

            labelQueue.setText(Integer.toString(mData.getQueue_time()));
            labelSendTimeApi.setText(Integer.toString(mData.getSend_time_api()));
            labelSendTimeGsm.setText(Integer.toString(mData.getSend_time_gsm()));
            labelSendTimeModem.setText(Integer.toString(mData.getSend_time_modem()));

            List<mDeviceInfo> devicesList = mData.getDevicesList(); //Из модели беру только настройки устройств.
            //Заполняю грид данными.
            ObservableList<mDeviceInfo> data = FXCollections.<mDeviceInfo>observableArrayList();
            data.addAll(devicesList);
            tableViewDeviceInfo.setItems(data);
            labelDetailInfo.setText("Кликни на строку таблицы для подробной информации");
        }
    }

    /**
     * Событие нажатия на кнопку "Настроить устройство."
     */
    @FXML
    public void buttonConfDeviceClick() {

        mDeviceInfo dev = tableViewDeviceInfo.getSelectionModel().getSelectedItem(); //Выделенная строка.

        if (dev == null) {
            showError("Ошибка", "", "Не выбрано устройство для настройки.");
            return;
        }

        try {

            //Показываем форму редактирования.
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(
                            "editDevForm/settingDevForm.fxml"
                    )
            );

            Stage stage = new Stage(StageStyle.DECORATED);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(
                    new Scene(loader.load())
            );

            settingDevFormController controller = loader.getController(); //Получаю контроллер.
            controller.initData(dev); //Передаю устройство которое редактируется.

            stage.setResizable(false);
            stage.setTitle("Настройка устройства.");
            stage.showAndWait();
            if (!controller.isDialogResult()) return; //Нажал отмена.

            dev = controller.getModel(); //Получаю измененные данные из формы редактирования.

            //Подготовка модели.
            mDevicesInfo model = new mDevicesInfo();
            ArrayList<mDeviceInfo> lines = new ArrayList<mDeviceInfo>();
            lines.add(dev);
            model.setDevicesList(lines);

            //Отправка команды сохранить настройку для устройства.
            labelDetailInfo.setText("Сохранение данных...");
            boolean IsGood = sms.setDevConfig(model);

            if (!IsGood) //Возникла ошибка в процессе.
            {
                showError("Ошибка", "", sms.getLastError());
                return;
            }

            buttonGetServerDataClick(); //Обновление данных.

        } catch (IOException e) {
            showError("Ошибка", "", "Форма редактирования не была загружена, исключение:" + e.getMessage());
        }
    }


    /**
     * Нажатие на кнопку "Получить типы устройств".
     */
    @FXML
    public void buttonGetDevTypesClick() {
        //Хост на котором работает смс сервер.
        mComboList v = (mComboList) comboBoxHosts.getValue();
        String host = v.getValue();
        sms.setServerHost(host);

        mDevTypes model = sms.getDevTypes();
        if (model == null) //Возникла ошибка в процессе.
        {
            showError("Ошибка", "", sms.getLastError());
            return;
        }

        //Заполняю грид данными.
        ObservableList<mDevType> data = FXCollections.<mDevType>observableArrayList();
        data.addAll(model.getDevTypes());
        tableViewDeviceType.setItems(data);
        //showConfirm("Информация","","Данные получены.");
    }

    /**
     * Нажатие на кнопку "Задать типы устройств".
     */
    @FXML
    public void buttonSetDevTypesClick() {

        mDevType line = tableViewDeviceType.getSelectionModel().getSelectedItem(); //Выделенная строка.
        if (line == null) {
            showError("Ошибка", "", "Не выбрано устройство для настройки.");
            return;
        }


        try {
            //Показываем форму редактирования.
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(
                            "editDevTypesForm/settingDevTypesForm.fxml"
                    )
            );

            Stage stage = new Stage(StageStyle.DECORATED);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(
                    new Scene(loader.load())
            );

            settingDevTypesFormController controller = loader.getController(); //Получаю контроллер.
            controller.initData(line); //Передаю модель для редактирования.

            stage.setResizable(false);
            stage.setTitle("Настройка устройства.");
            stage.showAndWait();
            if (!controller.isDialogResult()) return; //Нажал отмена.

            line = controller.getModel(); //Получаю модель из формы редактирования.

            //Отправка команды сохранить таблицу устройства.
            mDevTypes model = new mDevTypes(); //Подготовка модели.
            ArrayList<mDevType> lines = new ArrayList<mDevType>();
            lines.add(line);
            model.setDevTypes(lines);

            //Задаем хост на котором работает смс сервер.
            mComboList v = (mComboList) comboBoxHosts.getValue();
            String host = v.getValue();
            sms.setServerHost(host);

            //Отправка команды смс серверу.
            boolean IsGood = sms.setDevTypes(model);
            if (!IsGood) //Возникла ошибка в процессе.
            {
                showError("Ошибка", "", sms.getLastError());
                return;
            } else {
                buttonGetDevTypesClick();//Обновим данные.
                //showConfirm("Успешно", "", "Успешно");
            }


        } catch (IOException e) {
            showError("Ошибка", "", "Форма редактирования не была загружена, исключение:" + e.getMessage());
        }

    }

    /**
     * Получить параметры сервера. Только публичные данные.
     */
    @FXML
    public void buttonGetSrvParamClick() {
        //Хост на котором работает смс сервер.
        mComboList v = (mComboList) comboBoxHosts.getValue();
        String host = v.getValue();
        sms.setServerHost(host);

        mDevicesInfo mData = sms.getSrvSettings(); //Получаем модель данных.
        if (mData==null) //Возникла ошибка в процессе.
        {
            showError("Ошибка", "", sms.getLastError());
            return;
        }

        textBoxQ.setText(Integer.toString(mData.getQueue_time()));
        textBoxtimeOutApi.setText(Integer.toString(mData.getSend_time_api()));
        textBoxtimeOutGsm.setText(Integer.toString(mData.getSend_time_gsm()));
        textBoxtimeOutModem.setText(Integer.toString(mData.getSend_time_modem()));
        textBoxVrEnable.setText(Integer.toString(mData.getEnableVRdevices()));
    }


    /**
     * Задание настроек на сервере.  Только публичные данные.
     */
    @FXML
    public void buttonSetingSrvParamClick() {

        if (!checkTextBox(textBoxQ, "Интервал времени очереди")) return;
        if (!checkTextBox(textBoxtimeOutModem, "Макс. время отправки через модем:")) return;
        if (!checkTextBox(textBoxtimeOutGsm, "Макс. время отправки через gsm.")) return;
        if (!checkTextBox(textBoxtimeOutApi, "Макс. время отправки через api:")) return;

        int enblVr = 0;
        try {
            enblVr = Integer.parseInt(textBoxVrEnable.getText());
        } catch (Exception ex) {
            showError("Введите число в поле Включение виртуальных устройств.");
            return;
        }


        //Подготовка модели.
        mDevicesInfo model = new mDevicesInfo();
        model.setQueue_time(getIntValue(textBoxQ));
        model.setSend_time_modem(getIntValue(textBoxtimeOutModem));
        model.setSend_time_gsm(getIntValue(textBoxtimeOutGsm));
        model.setSend_time_api(getIntValue(textBoxtimeOutApi));
        model.setEnableVRdevices(enblVr);

        //Отправка команды сохранить настройку для устройства.
        boolean IsGood = sms.setSrvSettings(model);

        if (!IsGood) //Возникла ошибка в процессе.
        {
            showError("Ошибка", "", sms.getLastError());
            return;
        }

        buttonGetServerDataClick(); //Обновление данных.
    }

    /**
     * Проверяет значение на число и отрицательное число.
     *
     * @param control
     * @param info
     * @return
     */
    private boolean checkTextBox(TextField control, String info) {
        if (control.getText().isEmpty()) {
            showError("Ошибка", "", "Не заполнено поле " + info + " .");
            return false;
        }

        int timeQ = 0;
        try {
            timeQ = Integer.parseInt(control.getText());
        } catch (Exception ex) {
            showError("Введите число в " + info + ".");
            return false;
        }


        if ((timeQ < 0) || (timeQ == 0)) {
            showError(info + " не может быть отрицательным числом или нулем.");
            return false;
        }

        return true;
    }

    /**
     * Преобразовывает текст в число.
     *
     * @param control
     * @return
     */
    private int getIntValue(TextField control) {
        int timeQ = 0;
        try {
            timeQ = Integer.parseInt(control.getText());
        } catch (Exception ex) {
        }

        return timeQ;
    }


    public String inputStreamToString(InputStream is) throws IOException {
        StringBuilder sb = new StringBuilder();
        String line;
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        while ((line = br.readLine()) != null) {
            sb.append(line);
        }
        br.close();
        return sb.toString();
    }


    private void showError(String Title, String headerText, String text) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        //alert.initOwner(anchorPane.getScene().getWindow());   //В предупреждении используется значок родителя.
        alert.setTitle(Title);
        alert.setHeaderText(headerText);
        alert.setContentText(text);
        alert.showAndWait();
    }

    private void showConfirm(String Title, String headerText, String text) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        //alert.initOwner(anchorPane.getScene().getWindow());   //В предупреждении используется значок родителя.
        alert.setTitle(Title);
        alert.setHeaderText(headerText);
        alert.setContentText(text);
        alert.showAndWait();
    }

    private void showError(String text) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Ошибка");
        alert.setHeaderText("");
        alert.setContentText(text);
        alert.showAndWait();
    }

}



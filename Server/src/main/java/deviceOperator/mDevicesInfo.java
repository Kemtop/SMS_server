package deviceOperator;

import java.util.List;

/**
 * Модель для получения данных об устройствах на сервере.
 */
public class mDevicesInfo {

    //Публичные параметры.
    private int queue_time;
    private int send_time_modem;
    private int send_time_api;
    private int send_time_gsm;
    private int enableVRdevices; //Включение виртуальных устройств. 0-нет, 1-100 создание нужного количества.

    /**
     * Список устройств в системе.
     */
    List<mDeviceInfo> devicesList = null;

    public List<mDeviceInfo> getDevicesList() {
        return devicesList;
    }

    public void setDevicesList(List<mDeviceInfo> devicesList) {
        this.devicesList = devicesList;
    }

    public int getQueue_time() {
        return queue_time;
    }

    public void setQueue_time(int queue_time) {
        this.queue_time = queue_time;
    }

    public int getSend_time_modem() {
        return send_time_modem;
    }

    public void setSend_time_modem(int send_time_modem) {
        this.send_time_modem = send_time_modem;
    }

    public int getSend_time_api() {
        return send_time_api;
    }

    public void setSend_time_api(int send_time_api) {
        this.send_time_api = send_time_api;
    }

    public int getSend_time_gsm() {
        return send_time_gsm;
    }

    public void setSend_time_gsm(int send_time_gsm) {
        this.send_time_gsm = send_time_gsm;
    }

    public int getEnableVRdevices() {
        return enableVRdevices;
    }

    public void setEnableVRdevices(int enableVRdevices) {
        this.enableVRdevices = enableVRdevices;
    }
}

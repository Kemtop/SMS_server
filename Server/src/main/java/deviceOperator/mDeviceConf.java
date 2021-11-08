package deviceOperator;

/**
 * Модель настроек устройств. Читается и записывается в файл, используется для настройки при старте сервера.
 */
public class mDeviceConf {
    //Тип устройства.
    public  int devType; //1-Модем, 2-GSM шлюз 3 API
    private String equipmentId;//IMEI для модемов. Для других уникальный идентификатор. МАК адрес для шлюза.
    private int channel; //Канал в который включено устройство.
    private int work_flg; //Флаг включения / отключения устройства администратором.
    private int msg_max; //Количество одновременно возможных отправляемых сообщений.
    private String info; //поле name которое пишет администратор.

    public int getDevType() {
        return devType;
    }

    public void setDevType(int devType) {
        this.devType = devType;
    }

    public String getEquipmentId() {
        return equipmentId;
    }

    public void setEquipmentId(String equipmentId) {
        this.equipmentId = equipmentId;
    }

    public int getChannel() {
        return channel;
    }

    public void setChannel(int channel) {
        this.channel = channel;
    }

    public int getWork_flg() {
        return work_flg;
    }

    public void setWork_flg(int work_flg) {
        this.work_flg = work_flg;
    }

    public int getMsg_max() {
        return msg_max;
    }

    public void setMsg_max(int msg_max) {
        this.msg_max = msg_max;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }
}

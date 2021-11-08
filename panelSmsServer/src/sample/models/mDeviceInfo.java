package sample.models;

import io.netty.channel.ChannelHandlerContext;

/**
 * Информация об устройстве, используется для отправки сведений клиенту.
 */
public class mDeviceInfo {
    //Модель модема.
    private String ucode; //Для модема IMEI+Кодированный адрес модема в системе который выдает modemManager, для шлюзов мак адресс, для апи оператор ip адрес сервера оператора.
    //Название устройтсва.
    private String deviceInfo;
    //Тип устройства.
    private String Type; //Модем, GSM шлюз  API
    private int channel; //Канал в который включено устройство.
    private int now_cnt; //Количество отправляемых в данный момент времени сообщений.
    private int msg_cnt; //Количество отправленых сообщений.
    private int err_cnt; //Количество ошибок отправки сообщений.
    private int work_flag; //Флаг работы устройства-1-включено, 0- отключено администратором. Задается на клиентской стороне.
    private String server; //Ip на котором работает сервер.
    private int msg_max; //Количество одновременно возможных отправляемых сообщений.
    private String name; //Имя которое присвоил администратор. Задается на клиентской стороне.

    //Состояние работы устройства -1-не рабочий,0-cвободен, 1-получил команда,ждет обработки физическим устройством.
    private String status;
    private String usePorts; //USB порты которые использует модем.
    private String signalQuality;//Уровень сигнала для модемов.
    private String operatorName; //Наимерование оператора с которым работает устройство.
    private int prioritet; //Приоритет устройства.


    public int getNow_cnt() {
        return now_cnt;
    }

    public void setNow_cnt(int now_cnt) {
        this.now_cnt = now_cnt;
    }

    public int getMsg_cnt() {
        return msg_cnt;
    }

    public void setMsg_cnt(int msg_cnt) {
        this.msg_cnt = msg_cnt;
    }

    public int getErr_cnt() {
        return err_cnt;
    }

    public void setErr_cnt(int err_cnt) {
        this.err_cnt = err_cnt;
    }

    public int getWork_flag() {
        return work_flag;
    }

    public void setWork_flag(int work_flag) {
        this.work_flag = work_flag;
    }

    public int getMsg_max() {
        return msg_max;
    }

    public void setMsg_max(int msg_max) {
        this.msg_max = msg_max;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUcode() {
        return ucode;
    }

    public void setUcode(String ucode) {
        this.ucode = ucode;
    }

    public String getType() {
        return Type;
    }

    public void setType(String type) {
        Type = type;
    }

    public String getDeviceInfo() {
        return deviceInfo;
    }

    public void setDeviceInfo(String deviceInfo) {
        this.deviceInfo = deviceInfo;
    }

    public String getUsePorts() {
        return usePorts;
    }

    public void setUsePorts(String usePorts) {
        this.usePorts = usePorts;
    }


    public String getSignalQuality() {
        return signalQuality;
    }

    public void setSignalQuality(String signalQuality) {
        this.signalQuality = signalQuality;
    }

    public String getOperatorName() {
        return operatorName;
    }

    public void setOperatorName(String operatorName) {
        this.operatorName = operatorName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getChannel() {
        return channel;
    }

    public void setChannel(int channel) {
        this.channel = channel;
    }

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public int getPrioritet() {
        return prioritet;
    }

    public void setPrioritet(int prioritet) {
        this.prioritet = prioritet;
    }
}

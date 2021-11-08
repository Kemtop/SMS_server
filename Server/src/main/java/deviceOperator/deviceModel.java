package deviceOperator;

import io.netty.channel.ChannelHandlerContext;

import java.util.ArrayList;

/**
 * Модель описания найденного устройства.
 */
public class deviceModel {

    //Модель модема.
    private int addr; //Адрес модема в системе который выдает modemManager.
    private String modemModel; //Удалить не исп.
    //Тип устройства.
    public  int Type; //1-Модем, 2-GSM шлюз 3 API
    public String deviceInfo;
    private String equipmentId;//IMEI для модемов.
    private String usePorts; //USB порты которые использует модем.
    private String deviceState; //Состояние устройства. Для модема state: registered
    private String signalQuality;//Уровень сигнала для модемов.

    private int now_cnt; //Количество отправляемых в данный момент времени сообщений.
    private int msg_cnt; //Количество отправленных сообщений.
    private int err_cnt; //Количество ошибок отправки сообщений
    private boolean work_flg; //Флаг работы устройства. Включено/выключено администратором.
    private int msg_max; //Количество одновременно возможных отправляемых сообщений.
    private String name; //Символическое назавание которое задает админ системы.

    private String operatorName; //Наимерование оператора с которым работает устройство.
    //Состояние работы устройства -1-не рабочий,0-cвободен, 1-получил команда,ждет обработки физическим устройством.
    private int status;
    private int channel; //Канал в который включено устройство.
    private int prioritet; //Приоритет устройкства.
    private String[] cmd=null; //Команда с параметрами для выполнения устройством.
    private ChannelHandlerContext ctx=null; //Ссылка на объект TCP соединения.

    public int getAddr() {
        return addr;
    }

    public void setAddr(int addr) {
        this.addr = addr;
    }

    public String getModemModel() {
        return modemModel;
    }

    public void setModemModel(String modemModel) {
        this.modemModel = modemModel;
    }

    public String getDeviceInfo() {
        return deviceInfo;
    }

    public void setDeviceInfo(String deviceInfo) {
        this.deviceInfo = deviceInfo;
    }

    public String getEquipmentId() {
        return equipmentId;
    }

    public void setEquipmentId(String equipmentId) {
        this.equipmentId = equipmentId;
    }

    public String getusePorts() {
        return usePorts;
    }

    public void setusePorts(String ports) {
        this.usePorts = ports;
    }

    public String getDeviceState() {
        return deviceState;
    }

    public void setDeviceState(String deviceState) {
        this.deviceState = deviceState;
    }

    public String getSignalQuality() {
        return signalQuality;
    }

    public void setSignalQuality(String signalQuality) {
        this.signalQuality = signalQuality;
    }


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

    public boolean isWork_flg() {
        return work_flg;
    }

    public void setWork_flg(boolean work_flg) {
        this.work_flg = work_flg;
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

    public String getOperatorName() {
        return operatorName;
    }

    public void setOperatorName(String operatorName) {
        this.operatorName = operatorName;
    }

    public int getType() {
        return Type;
    }

    public void setType(int type) {
        Type = type;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getChannel() {
        return channel;
    }

    public void setChannel(int channel) {
        this.channel = channel;
    }

    public int getPrioritet() {
        return prioritet;
    }

    public void setPrioritet(int prioritet) {
        this.prioritet = prioritet;
    }

    public ChannelHandlerContext getCtx() {
        return ctx;
    }

    public void setCtx(ChannelHandlerContext ctx) {
        this.ctx = ctx;
    }

    public String[] getCmd() {
        return cmd;
    }

    public void setCmd(String[] cmd) {
        this.cmd = cmd;
    }
}

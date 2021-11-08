import io.netty.channel.ChannelHandlerContext;

import java.sql.Timestamp;


/**
 * Объект из техзадания:Строка таблицы Таблица «Очередь» - queue.
 */
public class mSenderQueue {
    private  int no; //Номер по п/п
    private int channel; //Код канала
    private String phone;//Номер теле-фона
    private String info;//Сооб-щение
    private int stts; // Ожидает-0; отправляется-1; обработано-2.
    private int res; //Отправлен или нет
    private int code; //Проставляет  метод send_msg()
    //Тип  устройства
    private String dev_type_code; //Из таблицы устройств.
    //Приоритет устройства
    private int dev_type_prior;  //Из таблицы типов устройств
    private Timestamp timestamp_;

    /**
     * Объект из канала текущего соединения с клиентом.Используется для отправки ответа клиенту.
     */
    ChannelHandlerContext ctx;

    public int getNo() {
        return no;
    }

    public void setNo(int no) {
        this.no = no;
    }

    public int getChannel() {
        return channel;
    }

    public void setChannel(int channel) {
        this.channel = channel;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public int getStts() {
        return stts;
    }

    public void setStts(int stts) {
        this.stts = stts;
    }

    public int getRes() {
        return res;
    }

    public void setRes(int res) {
        this.res = res;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getDev_type_code() {
        return dev_type_code;
    }

    public void setDev_type_code(String dev_type_code) {
        this.dev_type_code = dev_type_code;
    }

    public int getDev_type_prior() {
        return dev_type_prior;
    }

    public void setDev_type_prior(int dev_type_prior) {
        this.dev_type_prior = dev_type_prior;
    }

    public ChannelHandlerContext getCtx() {
        return ctx;
    }

    public void setCtx(ChannelHandlerContext ctx) {
        this.ctx = ctx;
    }

    public Timestamp getTimestamp() {
        return timestamp_;
    }

    public void setTimestamp(Timestamp timestamp_) {
        this.timestamp_ = timestamp_;
    }
}

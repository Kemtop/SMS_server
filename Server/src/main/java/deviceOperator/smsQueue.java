package deviceOperator;

import io.netty.channel.ChannelHandlerContext;

/**
 * Предназначена для постановки очередь запросов на отправку сообщений.
 */
public class smsQueue {

    private ChannelHandlerContext ctx=null; //Ссылка на объект TCP соединения.
    private int channel;
    private String phone;
    private String smsText;
    private int stts; // Ожидает отправки=1;   отправляется=2;  обработано(отправлен)=3.
    private long timeStamp; //Временный отпечаток в миллисекундах.

    public ChannelHandlerContext getCtx() {
        return ctx;
    }

    public void setCtx(ChannelHandlerContext ctx) {
        this.ctx = ctx;
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

    public String getSmsText() {
        return smsText;
    }

    public void setSmsText(String smsText) {
        this.smsText = smsText;
    }

    public int getStts() {
        return stts;
    }

    public void setStts(int stts) {
        this.stts = stts;
    }

    private String[] cmd=null; //Команда с параметрами для выполнения устройством.

    public long getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    public String[] getCmd() {
        return cmd;
    }

    public void setCmd(String[] cmd) {
        this.cmd = cmd;
    }

    /*
    no	            channel	        phone	        info	    			 dev_type_code	dev_type_prior
    Номер по п/п	Код канала	Номер теле-фона	Сооб-щение
   stts
    Ожидает;
отправляется;
обработано.

res  Отправлен или нет	code  Проставляет
 метод
 send_msg() 	 Из таблицы
 устройств	 Из таблицы
 типов устройств


     */
}

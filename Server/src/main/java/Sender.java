import io.netty.channel.ChannelHandlerContext;

import tcpServer.TcpServer;


import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.concurrent.Exchanger;

/**
 * Объект из техзадания:Объект «Отправитель сообщений» sender
 * Объект предназначен для отправки сообщений.
 * !!!!!!!!!!!! Удалить!!!!!!!!!!!!!!!!!!!!!!!
 */
public class Sender {
    private int timeInterval;//интервал времени, через которое будет проходить цикл обработки.
    private boolean runPocess; //Флаг для запуска или остановки runWork().
    private Exchanger<String> log;

    //Очередь из технического задания.
    private ArrayList<mSenderQueue> queue=new ArrayList<mSenderQueue>();

    /**
     * Передача объекта для ведения логов.
     * @param log
     */
    public void setLog(Exchanger<String> log) {
        this.log = log;
    }

    public Sender(int timeInterval) {
        this.timeInterval = timeInterval;
        runPocess = false;
    }


    /*
    Метод sender.send_msg()
    Отправляет сообщение в синхронном режиме методом постановки в очередь и ожидания отправки.
    Параметры: 			канал (_channel), номер телефона (_phone), сообщение(_info).
    Возвращаемое значение: 	нет.
    Алгоритм работы:
    1.	Добавляет в конец списка queue объект, созданный на основе параметров и запоминаем его номер.
    2.	Делаем запись в протокол: «сообщение _info на номер телефона _phone - поставлено в очередь»
    3.	Запускается цикл ожидания по времени не превышающий send_time_modem:
    •	Выполняется временная задержка длиной queue_time.
    •	Получаем строку по номеру и статусу «отправлен» из таблицы «очередь»
    •	Если строка не получена, то следующая итерация.
    •	Удаляем строку по номеру из таблицы «очередь»
    •	Возвращаем результат отправки.
    4.	Удаляем строку по номеру из таблицы «очередь»
    5.	Делаем запись в протокол «сообщение _info на номер телефона _phone - таймаут»
    6.	Что-то делаем с устройством (переинициализируем его или исключаем из таблицы устройств)
    7.	Возвращаем отрицательный результат отправки.
     */


    public void send_msg(String[] cmdArr,ChannelHandlerContext ctx) {
         //firstCommand ="sendSms;"+channel+";"+phoneNumber+";"+message; //Отправляемая команда серверу.

        try {
               if(cmdArr.length<4) //Пакет команды не может содержать менне 4х блоков данных.
               {
                   String answer="Ошибка 102: Пакет команды не может содержать меннее 4х блоков данных.";
                   TcpServer.returnError(ctx,answer);
                   return;
               }


              int channel=Integer.parseInt(cmdArr[1]);
              //Напиши проверки.
            //   private ArrayList<mSenderQueue> queue=new ArrayList<mSenderQueue>();
            mSenderQueue m=new mSenderQueue();
            m.setChannel(channel);
            m.setPhone(cmdArr[2]);
            m.setInfo(cmdArr[3]);
            m.setStts(0);//Ожидает.
            m.setCtx(ctx); //Объект для возврата сообщения tcp серверу.
            System.currentTimeMillis();
            Timestamp timestamp = new Timestamp(System.currentTimeMillis()) ; //Временный отпечаток.
            m.setTimestamp(timestamp);
            log.exchange("Сообщение ["+cmdArr[3]+"] на номер телефона ["+cmdArr[2]+"] для канала ["+cmdArr[1]+"] поставлено в очередь.");//Выводим данные в лог.



        } catch (Exception ex)
        {

        }




/*

               System.out.println("First massage=" + firstCommand);
               ChannelFuture f = ClBootstrap.connect(serverHost, serverPort).sync(); // Ожидает этого будущего, пока оно не будет сделано, и повторно бросает причину неудачи, если это будущее не удалось.
 */

    }


    public void send_msg(int channel, String phone, String info, ChannelHandlerContext ctx)
    {
        /*
        mSenderQueue m=new mSenderQueue();
        m.channel=channel;
        m.phone=phone;
        m.info=info;
        m.ctx=ctx;
        
         */
    }


    /*
    Запускается таймер с интервалом, указанном в параметре. В процедуре обработки таймера происходит следующее:
•	Запускается цикл по типам устройств (типы: MODEM, API, GSM)
o	Запускается цикл по сообщениям, находящимся в очереди для текущего типа устройства и имеющим статус «ожидает». В цикле выполняем шаги:
	Определяется список доступных устройств для канала сообщения, список сортируется по приоритету типа устройства.
	Если устройств нет, то следующая итерация.
	Первому сообщению проставляем статус «отправляется».
	Если тип устройства MODEM, то отправляем первое сообщение через модем send_modem().
	Если тип устройства API, то отправляем первое сообщение через API send_api().
	Если тип устройства MODEM, то отправляем первое сообщение через модем send_gsm().
     */

    public void runWork() {
        runPocess = true;
        while (runPocess) {
            try {
                // Запускается таймер с интервалом, указанном в параметре.
                Thread.sleep(timeInterval);
            } catch (Exception ex) {

            }

        }

    }

}

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import deviceOperator.deviceOperator;
import io.netty.channel.ChannelHandlerContext;
import tcpServer.TcpServer;
import tcpServer.mQueueCommand;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Exchanger;

/**
 * Логика считывающая команды от TCP сервера и передающая их на объект работающий с физическими устройствами.
 */
public class LayerAdapter {
    private  TcpServer srv;
    private  deviceOperator opr; //Объект управления физическими средствами отправки СМС.
    private  Exchanger<String> log;
    private boolean runGraber=true; //Флаг работы процесса передачи команд из очереди сервера, к устройствам.

    public LayerAdapter(TcpServer srv, deviceOperator opr, Exchanger<String> log) {
        this.srv = srv;
        this.opr = opr;
        this.log = log;
    }

    public void stop()
    {
        runGraber=false;
    }

    /**
     * Запускает процес передачи команд из очереди сервера, к устройствам,а также логику обработки очереди смс сообщений.
     */
    public void begin()
    {
        runGraber=true;
        while (runGraber) {
            //Есть ли не обработанные запросы к серверу.
            mQueueCommand srvData = srv.getRequest(); //Возвращает элемент из начала очереди TCP сервера.
            if (srvData != null) //Есть данные для обработки.
            {
                String[] wordArr = parceCmd(srvData.getCommand()); //Обработка команды.
                if ((wordArr == null) ||(wordArr.length==0)) //Не корректная комманда.
                {
                    //Возвращаем клиенту сообщение с ошибкой.
                    String answer="Ошибка 100: Не известная команда: "+srvData.getCommand();
                    srv.returnError(srvData.getCtx(),answer);
                    continue;
                }

                switch (wordArr[0])
                {
                    //Отправить сообщение.
                    case "sendSms":
                         // opr.sendSms(wordArr,srvData.getCtx()); //Отправка смс через отдельный поток с контролями.
                        opr.addSendSmsToQueue(wordArr,srvData.getCtx()); //Через очередь как в Т.З.
                        break;

                        //Получить информацию об устройствах в системе.
                    case "getDevInfo":
                        opr.getDeviceInfo(srvData.getCtx());
                        break;

                    //Сохраняет все настройки из модели.
                    case "setDevConfig" :
                        opr.setDevConfig(wordArr,srvData.getCtx());
                    break;

                    //Сохраняет таблицу устройств.
                    case "setDevTypes" :
                        opr.setDevTypes(wordArr,srvData.getCtx());
                        break;

                    //Возвращает таблицу устройств.
                    case "getDevTypes" :
                        opr.getDevTypes(wordArr,srvData.getCtx());
                        break;

                     //Возвращает только настройки сервера(публичные параметры).
                    case "getPublicParam":
                        opr.getPublicParam(srvData.getCtx());
                        break;

                    //Задает только настройки сервера(публичные параметры).
                    case "setPublicParam":
                        opr.setPublicParam(wordArr,srvData.getCtx());
                        break;

                    default:
                        String answer="Ошибка 101: Не известная команда: "+srvData.getCommand();
                        srv.returnError(srvData.getCtx(),answer);

                }

            }

            //Ищет в очереди сообщений ожидающие отправки, пытается их отправить.
            opr.processSmsQueue();


            try {

                Thread.sleep(500);
            } catch (Exception e)
            {

            }

             //Распарсить команду , отпраить ее в сендер.
        }

        System.out.println("Сервер остановлен.");

    }

    /**
     * Парсинг команды. Если команда содержит не корректные данные возвращает null.
     * @param cmd
     * @return
     */
    private String[] parceCmd(String cmd)
    {
        //В комманде нет символов ;
        if (cmd.indexOf(';')==-1) return null;

        String [] wordArr=cmd.split(";");
        return  wordArr;
    }



}

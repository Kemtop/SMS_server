package deviceOperator;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import io.netty.channel.ChannelHandlerContext;
import tcpServer.TcpServer;
import tcpServer.mQueueCommand;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.Exchanger;
import java.util.stream.Collectors;

/**
 * Класс управляющий всеми устройствами.
 * GSM модемами, шлюзами, api оператора связи.
 */
public class deviceOperator {

    // * Максимальное количество потоков которые могут отправлять сообщения одновременно через модемы.
    final int maxThreadForSendSms = 5;
    int maxThreadForSendSms_cnt; //Счетчик количество потоков которые сейчас отправляют сообщения.

    private Exchanger<String> log;

    private String lastError; //Сообщение об ошибке. Microsoft like style pattern.

    //Список устройств найденых в системе, а также фактический конвеер.
    private List<deviceModel> devicesList = null;
    //Объект для работы с GSM модемами.
    private modemManager mm = null;

    ArrayList<smsQueue> queueSms = new ArrayList<smsQueue>(); //Очередь для отправки смс сообщений. См. т.з.

    private boolean disableScan; //Флаг отключения процесса поиска новых устройств.

    //Словарь преобразования типов устройсв в строковое представление.
    // Используется для трансляции данных при получении информации об оборудовании.

    //Транслятор режимов работы устройства, 0-свободен.
    Map<Integer, String> mapDevWorkTypes = new HashMap<Integer, String>();

    private mDevTypes devTypes;//Данные о типах устойств и их приоритетах.
    private params pubParams = new params(); //Параметры тайм ауто работы устройств и очереди.


    //Номера модемов. Нумерует сам modemManager.
    private List<Integer> modemNum = new ArrayList<>();

    public deviceOperator(Exchanger<String> log) {
        maxThreadForSendSms_cnt = 0;//Сброс счетчика потоков.
        this.log = log;
        this.disableScan = false; //Разрешен процесс поиска новых устройств.
        devicesList = new ArrayList<deviceModel>();
        devTypes = new mDevTypes();

        mm = new modemManager(); //Транслятор для работы с modemManager.
        mm.setLog(log);
        mm.setDevTypes(devTypes); //Таблица типов устройств.

         /* Заполняет таблицу devTypes-кодовое имена устройств, приоритет, и код в системе.
           Необходимо если сервер не настраивался ни когда, или возникли проблемы с чтением файла конфигурации.
         */
        fillDevTypes();
        //Формирует переводчики информации.
        init_mapDevWorkTypes();
    }


    public String getLastError() {
        return lastError;
    }

    /**
     * Отключает процесс поиска новых устройств.
     */
    public void disableScan() {
        this.disableScan = true;
    }

    /**
     * Возвращает флаг процесса поиска новых устройств.
     *
     * @return
     */
    public boolean isDisableScan() {
        return disableScan;
    }


    /**
     * Заполняет словарь преобразования int  типов режимов работы в строковое понятное человеку представление.
     */
    private void init_mapDevWorkTypes() {
        mapDevWorkTypes.put(0, "Свободен");
        mapDevWorkTypes.put(-1, "Не зарегестирован в сети");
        mapDevWorkTypes.put(-2, "Физически отключен");
    }

    /**
     * Заполняет таблицу devTypes-кодовое имена устройств, приоритет, и код в системе.
     * Необходимо если сервер не настраивался ни когда, или возникли проблемы с чтением файла конфигурации.
     */
    private void fillDevTypes() {
        ArrayList<mDevType> lines = new ArrayList<mDevType>();

        mDevType l1 = new mDevType();
        l1.setCode("MODEM");
        l1.setPrior(3);
        l1.setSysType(1);
        lines.add(l1);

        mDevType l2 = new mDevType();
        l2.setCode("GSM");
        l2.setPrior(2);
        l2.setSysType(2);
        lines.add(l2);

        mDevType l3 = new mDevType();
        l3.setCode("API");
        l3.setPrior(1);
        l3.setSysType(3);
        lines.add(l3);

        devTypes.setDevTypes(lines);
    }


    /**
     * Проверяет установлено ли необходимое программное обеспечение.
     *
     * @return
     */
    public boolean checkSoftware() {

        if (!mm.mmVersion()) {
            lastError = mm.getLastError();
            return false; //Получаю текущую версию modemManager.
        }
        return true;
    }

    /**
     * Выполняет поиск доступных устройств в системе. Используется только при старте сервера.
     */
    public boolean scan() {
        try {
            //  log.exchange("Поиск устройств");

            // System.out.println(mm.getVersion());
            //Cent OS 1.6.10, Ubuntu  1.12.8
            //Получаю список подключенных подемов.
            List<deviceModel> listModem = mm.scan();
            //Для версии 1.6.10 CentOS некоторые модемы нужно включать.
            mm.enableAllNewModem(listModem);

            mergeDevices(listModem); //Добавляем найденный устройства в общую таблицу.
            //
            // Перенесено в  Scan modem manager setDefaultChannelPrior(); //Если нет конфига устройств, ставим канал по умолчанию и приоритет.

            //Тест
           /*
            deviceModel x_dev=new deviceModel();
            x_dev.setAddr(0);

            String cmd[]=new String[]{"ussd","*101#"};
            x_dev.setCmd(cmd);
            mm.getUSSD(x_dev);
*/

            //    List<deviceModel> devicesList1 =devicesList;
            //   int y=0;

        } catch (Exception e) {
            //return null;
        }


        return true;
    }


    /**
     * Читает все настройки сервера.
     */
    public void readSettings() {

        //Публичные параметры.
        if (!readSrvParam()) {
            //вывод в лог
            printTologParam(lastError, null);
        }

        //Типы устройств.
        if (!readDevTypesParam()) {
            //вывод в лог
            printTologParam(lastError, null);
        }

        updateDevPrioritet();//Задание приоритетов устройствам.

        //Настройки устройств.
        ArrayList<mDeviceConf> listFileConf = new ArrayList<mDeviceConf>();
        if (!readDeviceConfig(listFileConf)) {
            //вывод в лог
            printTologParam(lastError, null);
        }


        //На основании данных кофигурационного файла задает параметры устройствам в системе.
        if (!configDevices(listFileConf)) {
            printTologParam(lastError, null);
        }

    }


    /**
     * Чтение настроек сервера, публичные параметры.
     *
     * @return
     */
    private boolean readSrvParam() {

        File file = new File("./data/srvPrm.xml");
        if (!file.exists() || file.isDirectory()) {
            //Включаем виртуальные модем так как они есть в дефаулт настройках.
            //Заданы настройки для виртуальных модемов.
            if (pubParams.getEnableVRdevices() > 0) {
                operateVirtualModems(pubParams.getEnableVRdevices()); //Создание виртуальных модемов.
            }

            return true;//Файла нет.
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

            if (xml.length() == 0) return true; //Файл пуст.


            XmlMapper xmlMapper = new XmlMapper();
            mSrvParam fileConf = xmlMapper.readValue(xml, mSrvParam.class);

            //Задание параметров серверу.
            pubParams.setQueue_time(fileConf.getQueue_time());
            pubParams.setSend_time_gsm(fileConf.getSend_time_gsm());
            pubParams.setSend_time_api(fileConf.getSend_time_api());
            pubParams.setSend_time_modem(fileConf.getSend_time_modem());
            pubParams.setEnableVRdevices(fileConf.getEnableVRdevices());

            //Заданы настройки для виртуальных модемов.
            if (fileConf.getEnableVRdevices() > 0) {
                operateVirtualModems(fileConf.getEnableVRdevices()); //Создание виртуальных модемов.
            }

        } catch (Exception ex) {
            lastError = "Исключение в методе readSrvParam:" + ex.getMessage();
            return false;
        }

        return true;
    }


    /**
     * Чтение таблицы типов устройств.
     *
     * @return
     */
    private boolean readDevTypesParam() {

        File file = new File("./data/devTypes.xml");
        if (!file.exists() || file.isDirectory()) return true;//Файла нет.

        StringBuilder sb = new StringBuilder();
        String line;
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            br.close();
            String xml = sb.toString();

            if (xml.length() == 0) return true; //Файл пуст.


            XmlMapper xmlMapper = new XmlMapper();
            mDevTypes fileConf = xmlMapper.readValue(xml, mDevTypes.class);

            //Задание приоритетов для устройств.
            //Цикл по константным значениям в системе.
            for (mDevType d : devTypes.getDevTypes()) {
                //По настройкам в файле.
                for (mDevType cfg : fileConf.getDevTypes()) {
                    if (cfg.getCode().equals(d.getCode())) //Коды совпали.
                    {
                        d.setPrior(cfg.getPrior());
                        break;
                    }

                }

            }


        } catch (Exception ex) {
            lastError = "Исключение в методе readDevTypesParam:" + ex.getMessage();
            return false;
        }

        return true;
    }


    /**
     * На основании данных кофигурационного файла задает параметры устройствам в системе.
     */
    private boolean configDevices(ArrayList<mDeviceConf> listCfg) {
        try {
            for (mDeviceConf C : listCfg) //Цикл по строкам конфига.
            {
                for (deviceModel dev : devicesList) {
                    //Нашли нужное устройство.
                    if (dev.getEquipmentId().equals(C.getEquipmentId())) {
                        dev.setChannel(C.getChannel());
                        if (C.getWork_flg() == 0) dev.setWork_flg(false);
                        else dev.setWork_flg(true);
                        dev.setName(C.getInfo());
                        break;
                    }
                }

            }

        } catch (Exception ex) {
            lastError = "Исключение в методе configDevices:" + ex.getMessage();
            return false;
        }

        return true;
    }


    /**
     * Создает/удаляет виртуальные модемы для тестирования сервера.
     */
    public void operateVirtualModems(int val) {

        if (val < 0) val = 0; //Исключение пользовательских ошибок.
        if (val > 40) val = 40;

        if (val == 0) {

            mm.removeVirtualModems(devicesList); //Удаление виртуальных устройств.
            return;
        }

        //Иначе создание модемов.
        List<deviceModel> listModem = mm.createVirtualModem(val);
        mergeDevices(listModem); //Добавляем найденный устройства в общую таблицу.
        List<deviceModel> a = devicesList;


    }


    /**
     * Выполняет периодическое сканирование устройств.
     *
     * @return
     */
    public void periodicalScan() {

        int cnt = 0; //Счетик задержки.
        while (!disableScan) {

            //Для ускорнения остановки сервера, при большом периоде сканирования.
            while (!disableScan && (cnt < 5)) {
                try {
                    Thread.sleep(1000);
                } catch (Exception e) {
                }
                cnt++;
            }
            cnt = 0;

            //Может не существовать объекта с которым работает метод ниже.
            if (disableScan) {
                System.out.println("Поиск новых устройств остановлен.");
                disableScan = true; //Для анализа того что процесс остановился.
                return;
            }

            //Поиск новых модемов.
            mm.freshenUpDevList(devicesList);

            //Для версии 1.6.10 CentOS некоторые модемы нужно включать.
            mm.enableAllNewModem(devicesList);

        }

        System.out.println("Поиск новых устройств остановлен.");
        disableScan = true; //Для анализа того что процесс остановился.

    }


    /**
     * Добавляет в список устройств новые устройства.
     *
     * @param listDev
     */
    private void mergeDevices(List<deviceModel> listDev) {

        for (deviceModel dev : listDev) {
            devicesList.add(dev);
        }
    }


    /**
     * Устанавливает всем устройствам канал по умолчанию и приоритет.
     * Удалить, перенесено в модем менаджер скан.
     */
    private void setDefaultChannelPrior() {
        for (deviceModel dev : devicesList) {
            dev.setChannel(1);
            dev.setPrioritet(3);

        }

    }


    /**
     * Добавляет сообщения в очередь сообщений для отправки.
     *
     * @param param
     * @param ctx
     */
    public void addSendSmsToQueue(String[] param, ChannelHandlerContext ctx) {
        // Получение текущей даты
        Date date = new Date();
        // Этот метод возвращает время в миллисекундах
        long timeMilli = date.getTime();

        smsQueue sms = new smsQueue();
        sms.setStts(1); //Ожидает отправки.
        sms.setCtx(ctx);
        sms.setCmd(param);
        sms.setTimeStamp(timeMilli);

        queueSms.add(sms); //Добавляем в очередь.

        //Запись в протокол.
        printTologParam("Cообщение [" + param[2] + "] на номер телефона [" + param[3] + "] от [" + ctx.channel().remoteAddress() +
                "] поставлено в очередь. Сейчас в очереди " + Integer.toString(queueSms.size()) + ".", null);

    }


    /**
     * Поток для обработки очереди сообщений. Аналог sender т.з.
     * Метод ищет в очереди сообщений ожидающие отправки, пытается их отправить.
     */
    public void processSmsQueue() {
        //Перебираем очередь.
        if (queueSms.size() == 0) return;

        try
        {
            smsQueue sms = queueSms.stream()
                    .filter(x -> x.getStts() == 1)
                    .findFirst().get();

            if (sms != null) {
                sendSmstoFreeDev(sms); //Пытаемся отправить через доступные устройства.
            }
        }
        catch (Exception ex)
        {
            System.out.println("processSmsQueue ex:"+ex.getMessage());
        }


        /*
            for (smsQueue sms : queueSms) {

                if(sms.getStts()==1) //Ожидает отправки.
                {
                    sendSmstoFreeDev(sms); //Пытаемся отправить через доступные устройства.
                }

            }
*/
    }


    /**
     * Отправлять сообщение взятое из очереди через свободное устройство.
     *
     * @return
     */
    private void sendSmstoFreeDev(smsQueue sms) {
        String[] param = sms.getCmd(); //Команда.
        ChannelHandlerContext ctx = sms.getCtx();

        int channel = 0;
        //Канал является числом?
        try {
            channel = Integer.parseInt(param[1]);
        } catch (NumberFormatException e) {
            queueSms.remove(sms); //Удаляем объект из очереди.
            String answer = "Ошибка T104: Значение канала не является числом.[" + param[1] + "]";
            TcpServer.returnError(ctx, answer);
            printTologParam(answer, param);
            return;
        }


        //Не вышел ли тайм аут отправки сообщения?
        Date date = new Date();
        // Этот метод возвращает время в миллисекундах
        long nowStamp = date.getTime();

        long diff = nowStamp - sms.getTimeStamp();
        diff /= 1000; //Секунды.
        if (diff > pubParams.getSend_time_modem()) {
            queueSms.remove(sms); //Удаляем объект из очереди.
            String answer = "Ошибка T704: Cообщение [" + param[2] + "] на номер телефона [" + param[3] + "] от [" + ctx.channel().remoteAddress() +
                    "] таймаут(" + Integer.toString(pubParams.getSend_time_modem()) + ")";
            TcpServer.returnError(ctx, answer);
            printTologParam(answer, param);
            return;
        }

        if (devicesList == null) //В системе нет ни одного устройства.
            return;


        if (maxThreadForSendSms_cnt > maxThreadForSendSms) return; //Превышение лимита потоков для отправки сообщений.

        //Выбираем все устройства с требуемым каналом.
        List<deviceModel> groupDev = new ArrayList<deviceModel>();
        for (deviceModel dev : devicesList) {
            //Устройства с указанным каналом и устройства свободные, устройства не отключенны администратором системы.
            if ((dev.getChannel() == channel) && (dev.getStatus() == 0) && (dev.isWork_flg() == true)) {
                groupDev.add(dev);
            }
        }

        //Нет устройств для данного канала.
        if (groupDev.size() == 0) {
            return;
        }

        //Ищет самое приоритетное устройство если оно существует, если не существует найдется самое не приоритетное.
        deviceModel priorDev = null;

        //Поиск максимального приоритета заданного в таблице.
        int max_prior = -1000000;
        ArrayList<mDevType> tp = devTypes.getDevTypes();
        for (mDevType t : tp) {
            if (t.getPrior() > max_prior) max_prior = t.getPrior();
        }


        //Цикл по приоритетам
        for (int i = 1; i < max_prior + 1; i++) {
            //Цикл по устройствам.
            for (deviceModel curDev : groupDev) {
                //Нашли приоритетное устройство и устройство свободно(готово принимать команды).
                if ((curDev.getPrioritet() == i) && (curDev.getStatus() == 0)) {
                    priorDev = curDev;
                    break;
                }

            }
        }


        //Указан странный приоритет.
        if (priorDev == null) {
            queueSms.remove(sms); //Удаляем объект из очереди.
            String answer = "Ошибка T106: Не удалось найти устройство с нужным приоритетом.[" + param[1] + "]";
            TcpServer.returnError(ctx, answer);
            printTologParam(answer, param);
            return;
        }

        priorDev.setStatus(1); //Устройство получило команду и стало занятым.

        if (priorDev.getType() != 1) //устройство не модем.
        {
            queueSms.remove(sms); //Удаляем объект из очереди.
            String answer = "Ошибка T106A: Логика работы с устройствами отличными от GSM модемов заблокирована, обратитесь к разработчику!!!.";
            TcpServer.returnError(ctx, answer);
            printTologParam(answer, param);
            return;

        }

        priorDev.setCmd(param); //Передаю команду с параметрами для обработки исполняемым потоком.
        priorDev.setCtx(ctx); //Ссылка на соединение.
        final deviceModel busyDev = priorDev; //Передаем ссылку на наше устройство.

        //Запускаем поток который работает с устройством.
        //Поток в котором работает сервер.

        Thread sendThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {

                    //На основании типа устройства вызываем нужный метод.
                    switch (busyDev.getType()) {
                        //Устройство является модемом.
                        case 1:
                            sms.setStts(2); //Ставим статус "Отправляется".
                            busyDev.setNow_cnt(1);//Количество отправляемых в данный момент времени сообщений.
                            maxThreadForSendSms_cnt++; //Увеличение счетчиков.
                            mm.send_sms(busyDev);
                            maxThreadForSendSms_cnt--;
                            queueSms.remove(sms); //Удаляем объект из очереди.
                            break;
                    }
                } catch (Exception ex) {
                    System.out.println("Ex" + ex.getMessage());
                }

            }
        });
        sendThread.start();

    }


    /**
     * Отправка сообщения. Формат команды. "sendSms;"+channel+";"+phoneNumber+";"+message+'\n';
     *
     * @param param
     * @param ctx
     */
    public void sendSms(String[] param, ChannelHandlerContext ctx) {


        if (devicesList == null) //В системе нет ни одного устройства.
        {
            String answer = "Ошибка 103: Нет физических устройств. Подключите устройства и попробуйте позже.";
            TcpServer.returnError(ctx, answer);
            printTologParam(answer, param);
            return;
        }

        int channel = 0;
        //Канал является числом?
        try {
            channel = Integer.parseInt(param[1]);
        } catch (NumberFormatException e) {

            String answer = "Ошибка 104: Значение канала не является числом.[" + param[1] + "]";
            TcpServer.returnError(ctx, answer);
            printTologParam(answer, param);
            return;
        }


        //Выбираем все устройства с требуемым каналом.
        List<deviceModel> groupDev = new ArrayList<deviceModel>();
        for (deviceModel dev : devicesList) {
            //Устройства с указанным каналом и устройства свободные, устройства не отключенны администратором системы.
            if ((dev.getChannel() == channel) && (dev.getStatus() == 0) && (dev.isWork_flg() == true)) {
                groupDev.add(dev);
            }
        }

        //Нет устройств для данного канала.
        if (groupDev.size() == 0) {
            String answer = "Ошибка 105: Нет устройств для канала[" + param[1] + "] или все устройства заняты или администратор отключил требуемое устройство.";
            TcpServer.returnError(ctx, answer);
            printTologParam(answer, param);
            return;
        }

        //Ищет самое приоритетное устройство если оно существует, если не существует найдется самое не приоритетное.
        deviceModel priorDev = null;

        //Цикл по приоритетам
        for (int i = 1; i < 4; i++) {
            //Цикл по устройствам.
            for (deviceModel curDev : groupDev) {
                //Нашли приоритетное устройство и устройство свободно(готово принимать команды).
                if ((curDev.getPrioritet() == i) && (curDev.getStatus() == 0)) {
                    priorDev = curDev;
                    break;
                }

            }
        }


        //Указан странный приоритет.
        if (priorDev == null) {
            String answer = "Ошибка 106: Не удалось найти устройство с нужным приоритетом.[" + param[1] + "]";
            TcpServer.returnError(ctx, answer);
            printTologParam(answer, param);
            return;
        }

        priorDev.setStatus(1); //Устройство получило команду и стало занятым.

        if (priorDev.getType() != 1) //устройство не модем.
        {
            String answer = "Ошибка 106A: Логика работы с устройствами отличными от GSM модемов заблокирована, обратитесь к разработчику!!!.";
            TcpServer.returnError(ctx, answer);
            printTologParam(answer, param);
            return;

        }

        priorDev.setCmd(param); //Передаю команду с параметрами для обработки исполняемым потоком.
        priorDev.setCtx(ctx); //Ссылка на соединение.
        final deviceModel busyDev = priorDev; //Передаем ссылку на наше устройство.

        //Запускаем поток который работает с устройством.
        //Поток в котором работает сервер.

        Thread sendThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {

                    //На основании типа устройства вызываем нужный метод.
                    switch (busyDev.getType()) {
                        //Устройство является модемом.
                        case 1:
                            busyDev.setNow_cnt(1);//Количество отправляемых в данный момент времени сообщений.
                            mm.send_sms(busyDev);
                            break;
                    }
                } catch (Exception ex) {
                    System.out.println("Ex" + ex.getMessage());
                }

            }
        });
        sendThread.start();

    }

    /**
     * Возвращает сведения об устройствах в системе, и настройках.
     *
     * @param ctx
     */
    public void getDeviceInfo(ChannelHandlerContext ctx) {

        //Адрес на котором работает данные сервер, пример 192.168.10.129:3700
        String serverIp = ctx.channel().localAddress().toString();
        serverIp = modifyServerIp(serverIp);

        printTologParam("Получена команда вернуть сведения об устройствах(команда от " +
                ctx.channel().remoteAddress().toString() + ").", null);

        mDevicesInfo data = new mDevicesInfo();
        data.setDevicesList(fillModelInfo(serverIp)); //Устройства которые есть в системе.
        fillPublicParams(data); //Передаю публичные параметры.

        //Преобразование в json и отправка.
        try {
            ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
            String json = ow.writeValueAsString(data);
            TcpServer.returnJsonStr(ctx, json);

        } catch (Exception e) {
            lastError = "Возникло исключение:" + e.getMessage();
            printTologParam(lastError, null);
            TcpServer.returnError(ctx, lastError);
        }

    }


    /**
     * Убирает из адреса сервера порт и лишнее символы.
     *
     * @param ip
     * @return
     */
    private String modifyServerIp(String ip) {
        ip = ip.replace("/", "");
        int pos = ip.indexOf(':');
        ip = ip.substring(0, pos);
        return ip;
    }

    /**
     * Копирует настройки объекта(публичные параметры в Т.З.) в модель.
     *
     * @param model
     */
    private void fillPublicParams(mDevicesInfo model) {
        model.setQueue_time(pubParams.getQueue_time());
        model.setSend_time_modem(pubParams.getSend_time_modem());
        model.setSend_time_api(pubParams.getSend_time_api());
        model.setSend_time_gsm(pubParams.getSend_time_gsm());
        model.setEnableVRdevices(mm.getEnableVrModem());
    }

    /**
     * Формирует модель информации об устройствах.
     */
    private List<mDeviceInfo> fillModelInfo(String serverIp) {


        List<mDeviceInfo> out = new ArrayList<mDeviceInfo>();

        //Цикл по всем устройствам в системе.
        int cnt = 1;
        String ucode = "";
        for (deviceModel dev : devicesList) {

            mDeviceInfo info = new mDeviceInfo();
            info.setServer(serverIp);//Ip адрес на котором работает сервер.

            if (dev.getType() == 1) //Устройство модем.
            {
                ucode = dev.getEquipmentId() + "m" + Integer.toString(dev.getAddr()); //Адрес в модем менеджере.
            } else {
                ucode = dev.getEquipmentId(); //Уникальный код устройства для щлюза мак, для оператора ip.
            }

            info.setUcode(ucode); //Номер.

            String type = devTypes.getDevCode(dev.getType()); //Тип устройства.
            info.setType(type);

            info.setDeviceInfo(dev.getDeviceInfo()); //Модель устройтсва.
            info.setUsePorts(dev.getusePorts());
            info.setOperatorName(dev.getOperatorName());
            info.setSignalQuality(dev.getSignalQuality());

            info.setChannel(dev.getChannel()); //Канал.
            info.setPrioritet(dev.getPrioritet()); //Приоритет.

            String work = mapDevWorkTypes.get(dev.getStatus());
            info.setStatus(work);
            if (dev.isWork_flg()) info.setWork_flag(1);
            else info.setWork_flag(0);
            info.setMsg_max(dev.getMsg_max());
            info.setName(dev.getName());

            out.add(info);

        }

        return out;
    }


    /**
     * На основании переданной модели, настраивает и сохраняет устройства.
     *
     * @param param
     * @param ctx
     */
    public void setDevConfig(String param[], ChannelHandlerContext ctx) {
        //"setDevConfig;" + json + ";\n"; //Команда клиента.

        try {
            ObjectMapper mapper = new ObjectMapper();
            mDevicesInfo mData = mapper.readValue(param[1], mDevicesInfo.class);

            printTologParam("Получена команда сохранить настройки устройств(команда от " +
                    ctx.channel().remoteAddress().toString() +
                    ")...", null);

            //Сохраняет публичные данные.
            if (!savePublicParam(mData)) {
                printTologParam(lastError, param);
                TcpServer.returnError(ctx, lastError);
                return;
            }

            //Получили пустой список,значит нужно сохранить только публичные параметры.
            if ((mData.getDevicesList() == null) || (mData.getDevicesList().size() == 0)) {
                printTologParam("Сохранение настроек устройств(публичные данные) успешно выполнено (команда от " +
                        ctx.channel().remoteAddress().toString() + ").", null);
                TcpServer.returnOK(ctx); //Команда успешно выполнена.
                return;
            }

            //Получаем устройства которые требуется настроить.
            List<mDeviceInfo> confM = mData.getDevicesList();

            printTologParam("Количество настр. устройств=" + Integer.toString(confM.size()) +
                    ")...", null);

            //Задает параметры устройствам в системе, в реальном времени, на основании модели пришедшей от клиента.
            nowSetDevParam(confM, ctx);

            //Чтение файла конфига.
            ArrayList<mDeviceConf> fileConf = new ArrayList<mDeviceConf>();


            //Cчитываю конфиг устройств /data/devices.xml.
            if (!readDeviceConfig(fileConf)) {
                //Возникло исключение в методе.
                printTologParam(lastError, param);
                TcpServer.returnError(ctx, lastError);
            }

            //Задает параметры модели на основании полученных данных от клиента. Добавляет новые строки если их нет.
            setDeviceConfig(fileConf, confM);

            //Сохранение настроек в файл.
            if (!writeDeviceConfig(fileConf)) {
                //Возникло исключение в методе.
                printTologParam(lastError, param);
                TcpServer.returnError(ctx, lastError);
            }


            TcpServer.returnOK(ctx); //Команда успешно выполнена.
            printTologParam("Сохранение настроек устройств успешно выполнено (команда от " +
                    ctx.channel().remoteAddress().toString() + ").", null);


        } catch (Exception ex) {
            String answer = "Ошибка С103: Исключение в методе setDevModel:" + ex.getMessage();
            printTologParam(answer, param);
            TcpServer.returnError(ctx, answer);

        }

    }

    /**
     * Сохраняет публичные данные.
     *
     * @param data
     * @return
     */
    private boolean savePublicParam(mDevicesInfo data) {

        //Игнорирование модели которая не настраивает публичные параметры.
        if (data.getQueue_time() > 0)
            pubParams.setQueue_time(data.getQueue_time());

        if (data.getSend_time_modem() > 0)
            pubParams.setSend_time_modem(data.getSend_time_modem());

        if (data.getSend_time_api() > 0)
            pubParams.setSend_time_api(data.getSend_time_api());

        if (data.getSend_time_gsm() > 0)
            pubParams.setSend_time_gsm(data.getSend_time_gsm());

        operateVirtualModems(data.getEnableVRdevices()); //Вкл/откл VR модемов.

        //Копирование настроенных значений.
        mSrvParam prm = new mSrvParam();
        prm.setQueue_time(pubParams.getQueue_time());
        prm.setSend_time_api(pubParams.getSend_time_api());
        prm.setSend_time_gsm(pubParams.getSend_time_gsm());
        prm.setSend_time_modem(pubParams.getSend_time_modem());
        prm.setEnableVRdevices(mm.getEnableVrModem());

        //Сохранение данных в файл.
        if (!writeServerParam(prm)) return false;

        return true;
    }


    /**
     * Сохраняет параметры сервера в файл.
     *
     * @return
     */
    private boolean writeServerParam(mSrvParam prm) {
        try {
            XmlMapper xmlMapper = new XmlMapper();
            xmlMapper.enable(SerializationFeature.INDENT_OUTPUT); //Добавление переносов после тегов.

            //Cуществует ли каталог хранения настроек?
            Path path = Paths.get("./data");
            if (!Files.exists(path)) {
                //Папка не существует, создаем.
                File file = new File("./data");
                file.mkdir();
            }

            xmlMapper.writeValue(new File("./data/srvPrm.xml"), prm);

        } catch (Exception ex) {
            lastError = "Исключение в методе writeServerParam:" + ex.getMessage();
            return false;
        }

        return true;
    }


    /**
     * Задает параметры устройствам в системе, в реальном времени, на основании модели пришедшей от клиента.
     */
    private void nowSetDevParam(List<mDeviceInfo> confM, ChannelHandlerContext ctx) {
        //Цикл по настраиваемым устройствам.
        boolean find = false; //Думаю что не нашел.

        //Цикл по модели полученной от клиента.
        //Поиск устройства с указанным imei(или uid) и его настройка.
        for (mDeviceInfo m : confM) {

            //Преобразование идентификаторов для модем, в связи со спецификой отправки сведений о модеме. Уникальный код состоит из IMEI+m+код который присконил модем менеджер.
            String ucode = translateModemUcode(m.getUcode(), m.getType());
            m.setUcode(ucode);

            //Цикл по устройствам в системе.
            for (deviceModel dev : devicesList) {

                //Идентификаторы совпали.
                if (dev.getEquipmentId().equals(ucode)) {
                    dev.setChannel(m.getChannel()); //Задает канал.
                    //Включено/выключено администратором.
                    if (m.getWork_flag() == 0) dev.setWork_flg(false);
                    else
                        dev.setWork_flg(true);

                    //Количество одновременно возможных отправляемых сообщений. Можно задать только для устройств не модемов.
                    if (!m.getType().equals("MODEM"))
                        dev.setMsg_max(m.getMsg_max());

                    dev.setName(m.getName()); //Символическое имя.


                    find = true;
                    break;
                }
            }

            //Не найдено устройство которое хотят настроить.
            if (!find) {

                String answer = "Не удалось настроить устройство с equipmentId=" + ucode +
                        ". Похоже что устройство было отключено, а оператор не проконтролировал действие, не обновил статус устройств." +
                        "Задать канал можно только устройству существующему в системе. Обновите состояние устройств и попробуйте сново." +
                        "Команда пришла от клиента: " + ctx.channel().remoteAddress();
                printTologParam(answer, null);
            }
        }

    }


    /**
     * Возвращает IMEI модема на основании уникального кода который передается клиентской стороне.
     * Для других устройств возвращает строку без изменений.
     *
     * @param ucode пример 356342043782002m3
     * @param type  MODEM
     * @return 356342043782002
     */
    private String translateModemUcode(String ucode, String type) {
        if (!type.equals("MODEM")) return ucode; //Устройство не модем.

        int pos = ucode.indexOf('m');
        if (pos == -1) return ucode;  //Не найден разделитель для модемов. Возвращаем без изменений.

        ucode = ucode.substring(0, pos); //Берем только imei.
        return ucode;

    }


    /**
     * Считывает настройки устройств из файла.
     *
     * @return
     */
    private boolean readDeviceConfig(ArrayList<mDeviceConf> listConf) {

        File file = new File("./data/devices.xml");
        if (!file.exists() || file.isDirectory()) return true;//Файла нет.

        StringBuilder sb = new StringBuilder();
        String line;
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            br.close();
            String xml = sb.toString();

            if (xml.length() == 0) return true; //Файл пуст.


            XmlMapper xmlMapper = new XmlMapper();
            mDevicesConf fileConf = xmlMapper.readValue(xml, mDevicesConf.class);
            ArrayList<mDeviceConf> list = fileConf.getSetings();

            for (mDeviceConf l : list) {
                listConf.add(l);
            }

        } catch (Exception ex) {
            lastError = "Исключение в методе readDeviceConfig:" + ex.getMessage();
            return false;
        }

        return true;
    }

    private boolean writeDeviceConfig(ArrayList<mDeviceConf> listConf) {
        try {
            XmlMapper xmlMapper = new XmlMapper();
            xmlMapper.enable(SerializationFeature.INDENT_OUTPUT); //Добавление переносов после тегов.
            mDevicesConf m = new mDevicesConf();
            m.setSetings(listConf);

            //Cуществует ли каталог хранения настроек?
            Path path = Paths.get("./data");
            if (!Files.exists(path)) {
                //Папка не существует, создаем.
                File file = new File("./data");
                file.mkdir();
            }

            xmlMapper.writeValue(new File("./data/devices.xml"), m);

        } catch (Exception ex) {
            lastError = "Исключение в методе readDeviceConfig:" + ex.getMessage();
            return false;
        }

        return true;
    }

    /**
     * Настраивает модель на основании полученных данных от клиента, если listConf конфиг пустой(файла конфига нет, первый раз настраиваем)
     * добавляет строки устройств которые требуетя настроить.
     *
     * @param listConf
     * @param confM
     */
    void setDeviceConfig(ArrayList<mDeviceConf> listConf, List<mDeviceInfo> confM) {
        //Цикл по модели полученной от клиента.
        boolean find = false;
        for (mDeviceInfo m : confM) {

            find = false; //Думаю что не нашел.

            //Цикл по устройствам из конфига.
            for (mDeviceConf dev : listConf) {

                //Идентификаторы совпали.
                if (dev.getEquipmentId().equals(m.getUcode())) {
                    //dev.
                    dev.setChannel(m.getChannel()); //Задаем канал.
                    find = true;
                    break;
                }
            }

            //Не найдено устройство которое хотят настроить. Добавляем в список сохраняемый в файл.
            if (!find) {
                mDeviceConf dConf = new mDeviceConf();
                dConf.setChannel(m.getChannel());
                dConf.setEquipmentId(m.getUcode());
                listConf.add(dConf);
            }
        }
    }

    /**
     * Возвращат таблицу устройств.
     *
     * @param param
     * @param ctx
     */
    public void getDevTypes(String param[], ChannelHandlerContext ctx) {

        try {
            String answ = "Получена команда вернуть сведения о типах устройств(команда от " +
                    ctx.channel().remoteAddress().toString() + ").";
            printTologParam(answ, null);

            ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
            String json = ow.writeValueAsString(devTypes);
            TcpServer.returnJsonStr(ctx, json);

        } catch (Exception e) {
            String answ = "Исключение в методе getDevTypes:" + e.getMessage();
            printTologParam(answ, null);
            TcpServer.returnError(ctx, answ);
        }

    }

    /**
     * Задает параметры таблицы устройств.
     *
     * @param param
     * @param ctx
     */
    public void setDevTypes(String param[], ChannelHandlerContext ctx) {

        try {


            ObjectMapper mapper = new ObjectMapper();
            mDevTypes mData = mapper.readValue(param[1], mDevTypes.class);
            //Получили пустой список.
            if ((mData.getDevTypes() == null) || (mData.getDevTypes().size() == 0)) {
                String answer = "Ошибка У102: В переданном списке отсутствуют устройства для настройки.";
                printTologParam(answer, param);
                TcpServer.returnError(ctx, answer);
                return;
            }

            printTologParam("Получена команда настройки типов устройств(от " +
                    ctx.channel().remoteAddress().toString() +
                    ",количество настраиваемых строк=" + Integer.toString(mData.getDevTypes().size()) +
                    ").", null);

            if (!setDevTypes(mData)) //Изменяю настройки текущей системной таблицы.
            {
                printTologParam(lastError, param);
                TcpServer.returnError(ctx, lastError);
                return;
            }

            //Cинхронизирует приоритет устройств на основании таблицы устройств.
            updateDevPrioritet();
            //Сохранение настроек в файл.
            if (!writeDevTypes()) {
                printTologParam(lastError, param);
                TcpServer.returnError(ctx, lastError);
                return;
            }

            TcpServer.returnOK(ctx);
            printTologParam("Настройка типов устройств завершена(" +
                    ctx.channel().remoteAddress().toString() + ").", null);

        } catch (Exception e) {
            String answ = "Исключение в методе setDevTypes:" + e.getMessage();
            printTologParam(answ, null);
            TcpServer.returnError(ctx, answ);
        }
    }


    /**
     * Настраивает системную модель на основании данных пользователя.
     *
     * @param mDev
     * @return
     */
    private boolean setDevTypes(mDevTypes mDev) {
        ArrayList<mDevType> list = mDev.getDevTypes();
        //Цикл по всем переданным устройствам.
        boolean find = false;
        for (mDevType L : list) {
            //По текущим настройкам
            for (mDevType S : devTypes.getDevTypes()) {
                if (S.getCode().equals(L.getCode())) {
                    //Приоритет не может быть меньше единицы.
                    if (L.getPrior() < 1) {
                        lastError = "Ошибка Н104: Приоритет не может быть меньше единицы.Передано: " + Integer.toString(L.getPrior());
                        return false;
                    }

                    S.setPrior(L.getPrior());
                    find = true;
                    break;
                }
            }

            if (!find) {
                lastError = "Ошибка Н105: В системе не найдено устройства с кодом:[" + L.getCode() + "]";
                return false;
            }

        }

        return true;
    }

    /**
     * Сохраняет текущую таблицу типов устройств в файл.
     *
     * @return
     */
    private boolean writeDevTypes() {
        try {

            //Cуществует ли каталог хранения настроек?
            Path path = Paths.get("./data");
            if (!Files.exists(path)) {
                //Папка не существует, создаем.
                File file = new File("./data");
                file.mkdir();
            }
            XmlMapper xmlMapper = new XmlMapper();
            xmlMapper.enable(SerializationFeature.INDENT_OUTPUT); //Добавление переносов после тегов.
            xmlMapper.writeValue(new File("./data/devTypes.xml"), devTypes);

        } catch (Exception ex) {
            lastError = "Исключение в методе writeDevTypes():" + ex.getMessage();
            return false;
        }

        return true;
    }

    /**
     * Cинхронизирует приоритет устройств на основании таблицы устройств.
     */
    private void updateDevPrioritet() {
        //Цикл по устройствам в системе.
        int sysType = 0;
        for (deviceModel D : devicesList) {
            sysType = D.getType(); //Системный тип устройства. 1, 2, 3
            D.setPrioritet(devTypes.getPriorVal(sysType)); //Задает приоритет для данного типа устройтсва.
        }

    }

    //Возвращает только настройки сервера(публичные параметры).
    public void getPublicParam(ChannelHandlerContext ctx) {
        printTologParam("Получена команда вернуть только настройки сервера(публичные параметры),(команда от " +
                ctx.channel().remoteAddress().toString() + ").", null);

        mDevicesInfo data = new mDevicesInfo();
        fillPublicParams(data); //Передаю публичные параметры.

        //Преобразование в json и отправка.
        try {
            ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
            String json = ow.writeValueAsString(data);
            TcpServer.returnJsonStr(ctx, json);

        } catch (Exception e) {
            lastError = "Возникло исключение:" + e.getMessage();
            printTologParam(lastError, null);
            TcpServer.returnError(ctx, lastError);
        }

    }

    /**
     * //Задает только настройки сервера(публичные параметры).
     *
     * @param param
     * @param ctx
     */
    public void setPublicParam(String param[], ChannelHandlerContext ctx) {
        //"setDevConfig;" + json + ";\n"; //Команда клиента.

        try {
            ObjectMapper mapper = new ObjectMapper();
            mDevicesInfo mData = mapper.readValue(param[1], mDevicesInfo.class);

            printTologParam("Получена команда сохранить только настройки сервера(публичные параметры),(команда от " +
                    ctx.channel().remoteAddress().toString() +
                    ")...", null);

            //Сохраняет публичные данные.
            if (!savePublicParam(mData)) {
                printTologParam(lastError, param);
                TcpServer.returnError(ctx, lastError);
                return;
            }
            TcpServer.returnOK(ctx); //Команда успешно выполнена.
            printTologParam("Сохранение настроек параметра сервера успешно выполнено (команда от " +
                    ctx.channel().remoteAddress().toString() + ").", null);


        } catch (Exception ex) {
            String answer = "Ошибка E105: Исключение в методе setPublicParam:" + ex.getMessage();
            printTologParam(answer, param);
            TcpServer.returnError(ctx, answer);

        }
    }

    /**
     * Поток для отправки сообщений.
     */
    public void smsSender() {
        /*
 •	Запускается безусловный цикл (он же поток)
o	Запускается цикл по сообщениям, находящимся в очереди для текущего типа устройства и имеющим статус «ожидает». В цикле выполняем шаги:
	Определяется список доступных устройств для канала сообщения, список сортируется по приоритету типа устройства.
	Если устройств нет, то следующая итерация.
	Первому сообщению проставляем статус «отправляется».
	Если тип устройства MODEM, то отправляем первое сообщение через модем send_modem().
	Если тип устройства API, то отправляем первое сообщение через API send_api().
	Если тип устройства MODEM, то отправляем первое сообщение через модем send_gsm().
         */

        boolean runGraber = true;
        while (runGraber) {

            //Цикл по сообщениям в очереди сообщений.
            for (smsQueue M : queueSms) {
                //Сообщение имеет статус "ожидает".
                if (M.getStts() == 1) {

                }


            }


            //send_msg(int channel, String phone, String info, ChannelHandlerContext ctx)


            try {

                Thread.sleep(300);
            } catch (Exception e) {

            }

            //Распарсить команду , отпраить ее в сендер.
        }
    }


    /**
     * Выводит в лог сообщение об ошибке с параметрами.
     *
     * @param message
     * @param param
     */
    private void printTologParam(String message, String param[]) {

        if (param != null) {
            message = message + "\n Полученые параметры=\n";
            for (int i = 0; i < param.length; i++) {
                message += param[i] + ';';
            }
        }

        try {
            log.exchange(message);
        } catch (Exception e) {
            System.out.println("Warning! Exeption on method printTologParam" + e.getMessage());
        }
    }


}

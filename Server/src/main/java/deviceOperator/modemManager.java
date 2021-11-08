package deviceOperator;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.sun.jdi.event.ExceptionEvent;
import io.netty.channel.ChannelHandlerContext;
import tcpServer.TcpServer;

import java.io.Console;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.Exchanger;

/**
 * Низкоуровневая логика для работы с модемами.
 */
public class modemManager {

    private ShellExecutor se;//Объект для взаимодействия с командной строкой системы.
    private String lastError; //Последнее сообщени об ошибке.
    private String version; //Версия modemMamager. Для CentOS самая последняя 1.6.10 которая есть в репозитории.
    private Exchanger<String> log;
    private mDevTypes devTypes; //Данные о типах устойств и их приоритетах.
    private int enableVrModem; //Флаг включения режима создания виртуальных устройств.


    public modemManager() {
        se = new ShellExecutor();
    }

    public String getLastError() {
        return lastError;
    }

    public void setLog(Exchanger<String> log) {
        this.log = log;
    }

    public int getEnableVrModem() {
        return enableVrModem;
    }

    /**
     * Возвращает версию modemMamager, послет выполнения mmVersion().
     *
     * @return
     */
    public String getVersion() {
        return version;
    }

    public void setDevTypes(mDevTypes devTypes) {
        this.devTypes = devTypes;
    }

    /**
     * Получает версию modemManager который установлен в системе.
     *
     * @return
     */
    public boolean mmVersion() {
        if (!se.executeShellCommand("mmcli --version")) {
            lastError = se.getLastError();
            return false;
        }

        try {
            //Парсим версию.
            //Пример ответа: mmcli 1.12.8
            String str1 = se.getOut();
            int pos = str1.indexOf("mmcli");
            //mmcli не найден в системе.
            if (pos == -1) {
                lastError = "Ошибка: modemManager не найден в системе. Работа не возможна.";
                return false;
            }

            str1 = str1.substring(pos + 6, str1.length());
            pos = str1.indexOf('\n');
            str1 = str1.substring(0, pos);
            version = str1;
        } catch (Exception ex) {
            lastError = "В методе mmVersion возникло исключение:" + ex.getMessage();
            return false;
        }

        return true;
    }


    //parceModemNum(List<Integer> modemNum)

    /**
     * Поиск модемов.
     *
     * @return
     */
    public List<deviceModel> scan() {
        //Получение количества модемов в системе, и их номеров.
        List<Integer> modemNums = new ArrayList<Integer>();

        ShellExecutor se = new ShellExecutor();
        se.executeShellCommand("mmcli -L");
        se.parceModemNum(modemNums);
        if (modemNums.size() == 0) return null; //В системе нет устройств.

        ArrayList<deviceModel> devices = new ArrayList<deviceModel>(); //Модемы.

        for (int n : modemNums) //Цикл по количеству модемов в системе.
        {

            deviceModel m = getModemInfo(n); //Получаю информацию о модеме.
            //Модем был, но куда то исчез.
            if (m == null) {
                try {
                    log.exchange(
                            "Внимание! Система определила модем [mm_adr:" + Integer.toString(n) +
                                    "], но в процессе получения информации о модеме возникала ошибка:"
                                    + lastError); //Выводим данные в лог.
                } catch (Exception e) {
                    System.out.println("Modem manager scan exeption=" + e.getMessage());
                }

            }

            //Проверяем состояние модема, может в модеме нет сим карты.
            m.setAddr(n); //Число для взаимодействия с модемом через modemManager.
            m.setStatus(0); //Думаю что модем не занят и готов к работе.
            m.setChannel(1); //Устанавливаем канал по умолчанию.
            m.setPrioritet(devTypes.getPriorVal(devTypes.getSysType("MODEM"))); //Устанавливаем приоритет для устройств модемов.
            m.setMsg_max(1);//Количество одновременно возможных отпр сообщений.
            m.setWork_flg(true);//Устройство включено.

            if (!m.getDeviceState().equals("registered")) //Не зарегистрирован в сети.
                m.setStatus(-1); //Считаем модем не рабочий.

            devices.add(m);
        }

        return devices;
    }


    /**
     * Создает виртуальные модемы.
     *
     * @param val-количетсво модемов.
     */
    public List<deviceModel> createVirtualModem(int val) {
        ArrayList<deviceModel> devices = new ArrayList<deviceModel>(); //Модемы.

        for (int i = 0; i < val; i++) {
            deviceModel m = new deviceModel();
            //Модель модема.
            m.setDeviceInfo("VirtualModem GV105B");
            //IMEI модема.
            m.setEquipmentId("86153603019600" + Integer.toString(i + 1));
            m.setusePorts("PCI-EX16-" + Integer.toString(i + 1));
            m.setDeviceState("registered");

            int qv=(i + 1)*3;
            if(qv>100) qv=58;

            m.setSignalQuality(Integer.toString(qv));

            m.setOperatorName("X-Telecom");
            m.setType(devTypes.getSysType("MODEM"));//Ставим тип устройства-"GSM Modem".

            m.setAddr((i + 1)*-1); //Число для взаимодействия с модемом через modemManager.
            m.setStatus(0); //Думаю что модем не занят и готов к работе.
            m.setChannel(1); //Устанавливаем канал по умолчанию.
            m.setPrioritet(devTypes.getPriorVal(devTypes.getSysType("MODEM"))); //Устанавливаем приоритет для устройств модемов.
            m.setMsg_max(1);//Количество одновременно возможных отпр сообщений.
            m.setWork_flg(true);//Устройство включено.

            devices.add(m);

        }

        enableVrModem=val;//Созданы виртуальаные модемы с требуемым количеством.

        return devices;
    }

    /**
     * Удаляет все виртуальный модемы из системы
     */
    public void removeVirtualModems(List<deviceModel> dev)
    {
        dev.removeIf(value -> value.getAddr() < 0); //Удаляет только VR модемы, у которых отрицательный адрес.
        enableVrModem=0;
    }



    /**
     * Включает модем, нужно для старой версии 1.6.10 CentOs.
     *
     * @param num
     */
    private boolean enableModem(int num) {
        String cmd = "mmcli -m " + Integer.toString(num) + " -e"; //Включить модем.
        if (!se.executeBashCommand(cmd)) {
            printTologParam("Ошибка MMe1:Не удалось включить модем с адресом " + Integer.toString(num) +
                    ", ошибка: " + se.getLastError(), null);
            return false;
        }

        String ret = se.getOut();
        if (!ret.contains("successfully enabled the modem")) {
            printTologParam("Ошибка MMe2:Не удалось включить модем с адресом " + Integer.toString(num) +
                    ", не ожиданный ответ: " + ret, null);
            return false;
        }

        return true;
    }

    /**
     * Включает все не активные модемы. Обычно для версии модем менеджера 1.6.10, модем отключен и не зарегистр.
     *
     * @param curententDev
     */
    public void enableAllNewModem(List<deviceModel> curententDev) {
        for (deviceModel m : curententDev) {
            if (m.getStatus() == -1) //Включаем только не активные модемы.
            {
                String ans = "Включение модема " + m.getDeviceInfo() + " IMEI:" + m.getEquipmentId();
                printTologParam(ans, null);
                //Попытка влючить модем.
                if (enableModem(m.getAddr()))
                    m.setStatus(0); //Модем готов к работе.
            }

        }
    }

    /**
     * Получает информацию о модеме.
     *
     * @param modemNum
     * @return
     */
    public deviceModel getModemInfo(int modemNum) {
        try {

            //Получить информацию о модеме с данным адресом.
            String cmd = "mmcli -m " + Integer.toString(modemNum);
            if (!se.executeShellCommand(cmd)) {
                lastError = se.getLastError();
                return null;

            }

            String replaser = ""; //Разделитель информации. "---- "
            //Получаем резделитель для данной версии mmcli.
            String out = se.getOut();
            //Версия на Cent OS в начале ответа пишет.
            //\n /org/freedesktop/ModemManager1/Modem/2 (device id 'dc4bf44fd7c0cbae465b139a5dd51fc988ff213f')
            String oldAnsw = out.substring(1, 17); //Первые 16 символов, пропуская символ переноса.

            if (oldAnsw.equals("/org/freedesktop")) {
                //Отрезаем мусор.
                int a = out.indexOf(')');
                oldAnsw = out.substring(a + 2, out.length());
                out = oldAnsw;
            }


            int pos = out.indexOf('\n');
            replaser = out.substring(0, pos);
            replaser = replaser.trim();//Начальный проблел


            String[] parsed = out.split(replaser);

            //Заполняем модель на основании ответа.
            String topicName = "";//Название раздела который обрабатываем.
            String line = "";
            deviceModel m = new deviceModel();

            for (int i = 0; i < parsed.length; i++) {

                String str2 = parsed[i].trim();
                pos = str2.indexOf('|');
                if (pos == -1) continue; //Куда то делся разделитель.

                topicName = str2.substring(0, pos - 1);//Название раздела который обрабатываем.
                topicName = topicName.trim();
                line = str2.substring(pos, str2.length());
                String[] val = line.split("\n");


                //Парсю данные.
                switch (topicName) {

                    case "Hardware":
                        //Модель модема.
                        String manufacturer = getValFromSplit("manufacturer", val);
                        String model = getValFromSplit("model", val);
                        m.setDeviceInfo(manufacturer + ' ' + model);
                        //IMEI модема.
                        m.setEquipmentId(getValFromSplit("equipment id", val));
                        break;

                    case "System":
                        m.setusePorts(getValFromSplit("ports", val));

                        break;
                    case "Status":
                        m.setDeviceState(getValFromSplit("state", val));
                        m.setSignalQuality(getValFromSplit("signal quality", val));
                        break;
                    case "Modes":
                        break;
                    case "3GPP":
                        m.setOperatorName(getValFromSplit("operator name", val));
                        break;
                }
            }

            m.setType(devTypes.getSysType("MODEM"));//Ставим тип устройства-"GSM Modem".
            return m;

        } catch (Exception ex) {
            lastError = "В методе getModemInfo возникло исключение:" + ex.getMessage();
            return null;
        }
    }

    /**
     * Получает ключ значение из блока вида
     * manufacturer: huawei
     * |             model: E153
     * | firmware revision: 11.609.18.20.174
     * |         supported: gsm-umts
     * |           current: gsm-umts
     * |      equipment id: 354809043916234
     * разбитого по \n на строки.
     *
     * @param key
     * @return
     */
    String getValFromSplit(String key, String arr[]) {
        int pos;
        int pos1;
        String value = "";
        for (int j = 0; j < arr.length; j++) {
            String s = arr[j];
            if (!s.contains(key)) continue; //Если строка не содержит указанного ключа, пропускаем.

            s = s.replace('|', ' ');
            s = s.trim();

            pos = s.indexOf(key);
            value = s.substring(pos + key.length() + 2, s.length());
            break;
        }

        //Для старой версии CentOs 7 в значениях присутствуют кавычки.
        String outStr = value.replace("\'", "");

        return outStr;
    }


    /**
     * Инкрементирует счетчик ошибок и устанавливает в 0 количество отправляемых на данный момент сообщений.
     *
     * @param useDev
     */
    void incrementErrCnt(deviceModel useDev) {
        //Инкремент счетчика ошибок.
        int err = useDev.getErr_cnt();
        err++;
        useDev.setErr_cnt(err);
        useDev.setNow_cnt(0);//Cообщения в данный момент не отправляются.
    }


    /**
     * Отправка SMS на устройства. Результат возвращается клиенту через соединение.
     *
     * @param useDev
     */
    public void send_sms(deviceModel useDev) {
        try {
            log.exchange("Отправка сообщения [" + useDev.getCmd()[3] +
                    "] на номер [" + useDev.getCmd()[2] + "] через модем " +
                    Integer.toString(useDev.getAddr()) + " " + useDev.getDeviceInfo());//Выводим данные в лог.

            //Если модем является виртуальным.
            if (useDev.getAddr() < 0) {
                smstoVirtualModem(useDev); //Отправка через виртуальный модем(в файл).
                return;
            }

            //cmd=sendSms;1;0721112233;Hello people;
            //Формируем команду mmcli -m 0 --messaging-create-sms="text='Hello world',number='+1234567890'"
            String cmd = "mmcli -m " + Integer.toString(useDev.getAddr()) +
                    " --messaging-create-sms=\"text='" + useDev.getCmd()[3] + "',number='" + useDev.getCmd()[2] + "'\"";

            if (!se.executeBashCommand(cmd)) {
                //Инкрементирует счетчик ошибок и устанавливает в 0 количество отправляемых на данный момент сообщений.
                incrementErrCnt(useDev);
                String answer = "Ошибка MM1: Не удалось выполнить команду операционной системы:" + cmd + ", получена ошибка " + se.getLastError();
                TcpServer.returnError(useDev.getCtx(), answer);
                printTologParam(answer, null);
                return;
            }

            String retStr = se.getOut();

            //Ответ должен содержать строку "Successfully created new SMS:".
            if (!retStr.contains("Successfully created new SMS:")) {
                //Инкрементирует счетчик ошибок и устанавливает в 0 количество отправляемых на данный момент сообщений.
                incrementErrCnt(useDev);
                String answer = "Ошибка MM2: Не удалось выполнить команду операционной системы:" + cmd + ", получен не верный ответ "
                        + retStr;
                TcpServer.returnError(useDev.getCtx(), answer);
                printTologParam(answer, null);
                return;
            }

            //Удаляем Successfully created new SMS: /org/freedesktop/ModemManager1/SMS/10
            retStr = retStr.replace("Successfully created new SMS:", "");

            //String retStr="/org/freedesktop/ModemManager1/SMS/12 (unknown)";
            //String cmd="12121";
            //Ответ=/org/freedesktop/ModemManager1/SMS/12 (unknown)
            int pos = retStr.indexOf("SMS");
            if (pos == -1) //Странный ответ.
            {
                //Инкрементирует счетчик ошибок и устанавливает в 0 количество отправляемых на данный момент сообщений.
                incrementErrCnt(useDev);
                String answer = "Ошибка MM3: Не удалось выполнить команду операционной системы,не найден блок SMS:" + cmd + ", получен не верный ответ "
                        + retStr;
                TcpServer.returnError(useDev.getCtx(), answer);
                printTologParam(answer, null);
                return;
            }

            //Вырезаем номер по которому отправляется sms.
            String str = retStr.substring(pos + 4, retStr.length());
            //В старой версии есть (unknown)
            String str1 = str.replace("(unknown)", "");
            str = str1;

            pos = str.indexOf('\n');
            str = str.substring(0, pos);

            //Пример команды=mmcli -m 4 -s 25 --send
            // Ответ= successfully sent the SMS
            cmd = "mmcli -m " + Integer.toString(useDev.getAddr()) + " -s " +
                    str + " --send";

            if (!se.executeBashCommand(cmd)) {
                //Инкрементирует счетчик ошибок и устанавливает в 0 количество отправляемых на данный момент сообщений.
                incrementErrCnt(useDev);
                String answer = "Ошибка MM4: Не удалось отправить  SMS, ошибка=" + se.getLastError();
                TcpServer.returnError(useDev.getCtx(), answer);
                printTologParam(answer, null);
                return;
            }

            retStr = se.getOut();
            //Содержит не верный ответ.
            if (!retStr.contains("successfully sent the SMS")) {

                //Инкрементирует счетчик ошибок и устанавливает в 0 количество отправляемых на данный момент сообщений.
                incrementErrCnt(useDev);
                String answer = "Ошибка MM5: Не удалось отправить SMS , не верный ответ модема=" + retStr;
                TcpServer.returnError(useDev.getCtx(), answer);
                printTologParam(answer, null);
                return;
            }

            log.exchange("Успешно отправлено сообщение [" + useDev.getCmd()[3] +
                    "] на номер [" + useDev.getCmd()[2] + "] через модем " +
                    Integer.toString(useDev.getAddr()) + " " + useDev.getDeviceInfo());//Выводим данные в лог.  //Successfully created new SMS:

            useDev.setNow_cnt(0);//Cообщения в данный момент не отправляются.
            //Увеличиваем количество отправленных сообщений.
            int cnt1 = useDev.getMsg_cnt();
            cnt1++;
            useDev.setMsg_cnt(cnt1);

            TcpServer.returnOK(useDev.getCtx()); //Возвращаем клиенту ответ успешного выполнения команды.

            //Если сканер устройств(переодически проверяет устройства), не посчитал устройство не рабочим(ставит -2), тогда обновляем статус.
            if (useDev.getStatus() > 0) {
                useDev.setStatus(0); //Устройство свободно для получения команд.
            }

        } catch (Exception e) {
            System.out.println("modem manager thread exeption" + e.getMessage());
        }


            /*
            mmcli -m 0 --messaging-create-sms="text='Hello world',number='+1234567890'"
            Successfully created new SMS:
            /org/freedesktop/ModemManager1/SMS/12 (unknown)
	        mmcli -m 3 -e
        	https://itgrenade.1wordpress.com/2016/09/14/use-mmcli-for-sending-sms/
        	mmcli -m 4 --messaging-create-sms="text='Привет как дела',number='+380721081867'"
            mmcli -m 4 -s 25 --send
            successfully sent the SMS
            mmcli -m 4 --3gpp-ussd-initiate="*101#"
             */


    }

    /**
     * Отправка сообщения через виртуальный модем.
     *
     * @param useDev
     */
    public void smstoVirtualModem(deviceModel useDev) {
        try {
            //Cуществует ли каталог хранения настроек?
            Path path = Paths.get("./VirtualModems");
            if (!Files.exists(path)) {
                //Папка не существует, создаем.
                File file = new File("./VirtualModems");
                file.mkdir();
            }

            String filePath = "./VirtualModems/m" + Integer.toString(useDev.getAddr()) + ".sms";

            String text = "";
            for (int i = 2; i < 4; i++) {
                text += useDev.getCmd()[i] + " ";
            }



            FileOutputStream logFile = new FileOutputStream(filePath, true);
            String timeStamp = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(Calendar.getInstance().getTime());
            text = timeStamp + " " + text + "\r\n";
            logFile.write(text.getBytes(Charset.forName("UTF-8")));
            logFile.close();

            Thread.sleep(1000);

            log.exchange("Успешно отправлено сообщение [" + useDev.getCmd()[3] +
                    "] на номер [" + useDev.getCmd()[2] + "] через модем " +
                    Integer.toString(useDev.getAddr()) + " " + useDev.getDeviceInfo());//Выводим данные в лог.  //Successfully created new SMS:

            useDev.setNow_cnt(0);//Cообщения в данный момент не отправляются.
            //Увеличиваем количество отправленных сообщений.
            int cnt1 = useDev.getMsg_cnt();
            cnt1++;
            useDev.setMsg_cnt(cnt1);

            TcpServer.returnOK(useDev.getCtx()); //Возвращаем клиенту ответ успешного выполнения команды.

            //Если сканер устройств(переодически проверяет устройства), не посчитал устройство не рабочим(ставит -2), тогда обновляем статус.
            if (useDev.getStatus() > 0) {
                useDev.setStatus(0); //Устройство свободно для получения команд.
            }


        } catch (Exception ex) {
            lastError = "Исключение в методе readDeviceConfig:" + ex.getMessage();
            TcpServer.returnError(useDev.getCtx(),lastError);

        }

    }


    public void getUSSD(deviceModel useDev) {
        String cmd = "mmcli -m " + Integer.toString(useDev.getAddr()) + " --3gpp-ussd-initiate=\"" + useDev.getCmd()[1] + "\"";

        if (!se.executeBashCommand(cmd)) {

            String answer = "Ошибка MMUSD1: Не удалось выполнить USSD, ошибка=" + se.getLastError();
            TcpServer.returnError(useDev.getCtx(), answer);
            printTologParam(answer, null);
            return;
        }

        //Ответ=USSD session initiated; new reply from network: 'è¤Δ@C@Φ@ òD!@ùAæè¿ΔhC Δ@:@@£@ù@Ψ@l@8¥@ò@R@T$ '
        String retStr = se.getOut();
        //Содержит не верный ответ.
        if (!retStr.contains("USSD session initiated; new reply from network:")) {
            String answer = "Ошибка MMUSD2: Не удалось выполнить USSD,не верный ответ=" + retStr;
            TcpServer.returnError(useDev.getCtx(), answer);
            printTologParam(answer, null);
            return;
        }

        retStr = retStr.replace("USSD session initiated; new reply from network:", "");
        int pos = retStr.indexOf('\'');

        if (pos == -1) {
            String answer = "Ошибка MMUSD3: Не удалось выполнить USSD,не верный ответ=" + retStr;
            TcpServer.returnError(useDev.getCtx(), answer);
            printTologParam(answer, null);
            return;
        }

        String str = retStr.substring(pos + 1, retStr.length());
        pos = str.indexOf('\'');
        retStr = str.substring(0, pos);


        byte[] arr = retStr.getBytes(StandardCharsets.UTF_8);
        String s_pdu = new String(arr, StandardCharsets.US_ASCII);


        //mmcli -m 0 --3gpp-ussd-cancel
        //mmcli (--command=AT+CSCS=?
    }
/*
Listing your modems: mmcli -L. This will show a modem device path like /org/freedesktop/ModemManager1/Modem/12 and you can use the number at its end to specify the modem to use after the -m option in the following commands.

Showing attributes of your modem: mmcli -m 12.

Enabling the modem (needed before using it for USSD): mmcli -m 12 -e

Starting a USSD session. For example, for Ncell this command shows the main USSD menu: mmcli -m 12 --3gpp-ussd-initiate="*100#"

Responding to a USSD menu. After the session is started, you may use a command like this to respond, here using option 1: mmcli -m 12 --3gpp-ussd-respond="1"

Canceling the USSD session on the given modem: mmcli -m 12 --3gpp-ussd-cancel.

Obtaining the status of all USSD sessions (of all available modems): mmcli --3gpp-ussd-status.

With most hardware, this should work properly and im
 */


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


    /**
     * Анализирует текущие подключенные модемы, добавляет новые, удаляет отключенные в общем списке устройств.
     *
     * @return
     */
    public void freshenUpDevList(List<deviceModel> curententDev) {
        try {

            //Удаляет из списка устройства помеченные как отключенные модемы в прошлой итерации.
            //Сразу делать нельзя, так как модем может быть частично рабочим, и другой поток может отправлять сообщение.
            //Нужно сначала пометить устройство как не рабочее, запретив дальнейшее использование, а в следущем скане удалить его
            // тогда точно ни кто не будет с ним работать(если ни кто не установил не корректное время сканирования).
            clearDisabledDev(curententDev);

            List<deviceModel> listModem = scan(); //Получение текущего списка модемов в системе.
            if (listModem == null) //Модемов нет.
            {
                //Пропуск виртуальных модемов.
                if (curententDev.size() == 0) printTologParam("Внимание! В системе нет модемов.", null);

                //Удаление всех модемов.
                for (deviceModel devA : curententDev) {
                    if (devA.getType() == 1) //Устройство модем.
                    {
                        if (devA.getAddr() < 0) continue; //пропуск виртуальных модемов, у них адрес отрицательный.
                        String answ = "Модем " + devA.getDeviceInfo() + ",mm_addr=" + Integer.toString(devA.getAddr()) +
                                ",IMEI=" + devA.getEquipmentId() + " отключен.";
                        printTologParam(answ, null);
                        devA.setStatus(-2); //Модем был физически отключен из системы.
                    }
                }
                return;
            }


            boolean find = false;
            //Поиск отключенных устройств. Очередность циклов обязательна-для ускорения работы второго(поиск новых устройств) цикла.

            //Цикл по устройствам существующим в системе.
            for (deviceModel eDev : curententDev) {
                String imei = eDev.getEquipmentId(); //IMEI устройства которое есть в системе.
                find = false;//Думаю что не найду.

                if (eDev.getAddr() < 0) continue; //пропуск виртуальных модемов, у них адрес отрицательный.

                //Цикл по только что найденным устройствам.
                for (deviceModel d : listModem) {
                    //Адрес модем менеджер присваивает уникальный, на всякий случай сравним IMEI.
                    if (imei.equals(d.getEquipmentId())) //Одинаковые IMEI.
                    {
                        find = true;
                        break;
                    }
                }

                //Модема уже нет в системе.
                if (!find) {
                    eDev.setStatus(-2);
                    String answ = "Модем " + eDev.getDeviceInfo() + ",mm_addr=" + Integer.toString(eDev.getAddr()) +
                            ",IMEI=" + eDev.getEquipmentId() + " был отключен.";
                    printTologParam(answ, null);
                }

            }


            //Поиск новых устройств.

            //Цикл по только что найденным устройствам.
            for (deviceModel d : listModem) {
                if (d.getAddr() < 0) continue; //пропуск виртуальных модемов, у них адрес отрицательный.

                String imei = d.getEquipmentId(); //IMEI устройства которое мы только что нашли сканером.
                find = false; //Думаю что устройства нет в системе.

                //Цикл по устройствам существующим в системе.
                for (deviceModel eDev : curententDev) {
                    if (eDev.getStatus() == -2) continue; //Пропуск только что отключенных устройств.

                    if (eDev.getEquipmentId().equals(imei)) {
                        find = true;
                        break;
                    }
                }

                //Появилось новое устройство. Добавляем в общий список устройств.
                if (!find) {
                    String answ = "Найден новый модем " + d.getDeviceInfo() + ",mm_addr=" + Integer.toString(d.getAddr()) +
                            ",IMEI=" + d.getEquipmentId() + ".";
                    printTologParam(answ, null);
                    curententDev.add(d);
                }

            }


        } catch (Exception ex) {

            printTologParam("Возникло исключение в методе freshenUpDevList,=" + ex.getMessage(), null);
        }


    }

    /**
     * Удаляет из списка устройств помеченные как отключенные модемы.
     */
    private void clearDisabledDev(List<deviceModel> curententDev) {
        if (curententDev.size() == 0) return; //Нет устройств.
        for (deviceModel devA : curententDev) {
            if ((devA.getType() == 1) && (devA.getStatus() == -2)) //Устройство модем и было отключено.
            {
                curententDev.remove(devA); //Удаляю элементы.
            }
            if (curententDev.size() == 0) return;
        }

    }


}

import deviceOperator.deviceOperator;
import tcpServer.TcpServer;

import java.util.Scanner;
import java.util.concurrent.Exchanger;

import sun.misc.Signal;
import sun.misc.SignalHandler;
import uSignalHandler.DiagnosticSignalHandler;
import uSignalHandler.stopDelagete;


public class Main {
    static  int debug_val=0; //Параметр для поиска исключения на CentOs.
    static  boolean enableDbug=true; //Включить вывод сообщений.
    public static void main(String[] args) throws Exception {
        try {

            //Узнаю версию java.
            String javaVersion = System.getProperty("java.version");
            System.out.println("Приложение запущено JVM версии=" + javaVersion);

            //Читаем конфиг.
            Config cfg = new Config();
            if (!cfg.read()) return;

            LogWorker Log = new LogWorker(); //Ведет лог действий сервера.
            if (!Log.init(cfg.getLogFilePath())) {
                System.out.println("Ошибка: Не удалось инициализировать лог файл.");
                return;
            }


            //Объект для обмена данными между потоком логирования.
            Exchanger<String> logExchange = new Exchanger<String>();
            Log.setExchanger(logExchange); //Передаю объект для обмена данными между потоками.

            Thread logThread = new Thread(Log);
            logThread.setDaemon(true);
            logThread.start(); //Запуск потока логирования.


            //Список данных необходимых для вывода в лог.


            deviceOperator devOpr = new deviceOperator(logExchange); //Объект управления всеми средствами рассылки смс.
            //devOpr.setLog(;
            //Проверка наличия всего необходимого программного обеспечения для работы  deviceOperator.
            if (!devOpr.checkSoftware()) {
                logExchange.exchange(devOpr.getLastError());//Выводим данные в лог.
                return;
            }

            //Читает все настройки сервера.
            devOpr.readSettings();
            devOpr.scan(); //Поиск устройств установленных в системе.

            //runSmsFromQueueThreads(int val)

            int port = 3700;
            //Сервер для приема команд по сети.
            TcpServer srv = new TcpServer(port);

            //Поток в котором работает сервер.
            Thread serverThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        srv.run();
                    } catch (Exception ex) {

                    }

                }
            });

            serverThread.setDaemon(true);
            serverThread.start();
            //System.out.println("TCP сервер запущен.");
            logExchange.exchange("СМС сервер запущен.");//Выводим данные в лог.

            //Забирает из очереди TCP серведа данные, обрабатывает и доставляет конечным исполнителям.
            LayerAdapter adapter = new LayerAdapter(srv, devOpr, logExchange);

            //Поток в котором работает адаптер.
            Thread adapterThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        adapter.begin();
                    } catch (Exception ex) {
                        System.out.println("Exception in adapter:" + ex.getMessage());
                    }

                }
            });
            adapterThread.setDaemon(true);
            adapterThread.start();

            //Запускаем поток сканирующий новые устройства или анализирующий что устройство отвалилось.
            Thread scanThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    devOpr.periodicalScan();
                }
            });
            scanThread.start();


            //Опрос потока ввода для возможности остановки сервера.
            System.out.println("Для остановки сервера введите stop и нажмите Enter в окне терминала.");
            Scanner userInput = new Scanner(System.in);



            //Обработка сигналов ОС linux.

            //Обработчик сингналов.
            SignalHandler signalHandler = new SignalHandler() {
                @Override
                public void handle(Signal sig) {
                    System.out.println("Получен сигнал " + sig.getName());

                    System.out.println("Остановка сервера...");
                    logThread.interrupt(); //Остановка потока ведения логов.
                    srv.stop(); //Остановка TCP сервера.
                    devOpr.disableScan(); //Остановка процесса поиска новых устройств.
                    adapter.stop();
                }
            };
            DiagnosticSignalHandler.install("TERM", signalHandler);
            DiagnosticSignalHandler.install("INT", signalHandler);
            DiagnosticSignalHandler.install("ABRT", signalHandler);


            boolean runAsService=false; //Флаг запуска приложения как сервиса.
            while (true) {
                Thread.sleep(1000);
                String str ="";

                if(!runAsService)
                {
                    //При запуске сервера как службы возникает исключение No line found.
                    try {
                        str = userInput.nextLine();
                    }catch (Exception rr){
                        runAsService=true;
                    }
                }

                if (str.equals("stop") == true) {
                    System.out.println("Остановка сервера...");
                    logThread.interrupt(); //Остановка потока ведения логов.
                    srv.stop(); //Остановка TCP сервера.
                    devOpr.disableScan(); //Остановка процесса поиска новых устройств.

                    adapter.stop();
                    break;
                }
            }
        }catch (Exception ex)
        {
                String err="Исключение в Main, "+
                        Integer.toString(ex.getStackTrace()[0].getLineNumber())+":"+ex.getMessage();
                //На линукс все не влазит в строку. Выводим кусками.
            String arr[]=err.split("(?<=\\G.{20})");

             for (int i=0;i<arr.length;i++)
             {
                 System.out.println(arr[i]);
             }
        }
    }

    /**
     * Отладочные точки для консоли.
     */
  static   void printDebug(String str)
    {
        if(!enableDbug) return;
        System.out.println(str);

    }


}

/*
sudo adduser USERNAME dialout -дает права.

Проверка работы
systemctl list-dependencies multi-user.target | grep Modem

mmcli -L показывает список модемов. Модемы usb коннектит автоматом.
ответ команды(искал 23 секунды):
/org/freedesktop/ModemManager1/Modem/2 [huawei] E153


mmcli -M    Список доступных модемов и монитbyu модемов добавленных или удаленных.

Отправка сообщения.
/org/freedesktop/ModemManager1/Modem/3 [huawei] E153

список модемов:
1
2
3
$ mmcli -L
Found 1 modems:
    /org/freedesktop/ModemManager1/Modem/0 [huawei] E176
получить идентификатор модема из вывода (вот он: 0)
включить модем:
1
2
$ sudo mmcli -m 0 -e
successfully enabled the modem
создать смс:
1
2
3
$ sudo mmcli -m 0 --messaging-create-sms="text='Hello world',number='+1234567890'"
Successfully created new SMS:
    /org/freedesktop/ModemManager1/SMS/12 (unknown)
	
	mmcli -m 3 -e

	https://itgrenade.1wordpress.com/2016/09/14/use-mmcli-for-sending-sms/
	
	mmcli -m 4 --messaging-create-sms="text='Привет как дела',number='+380721081867'"
mmcli -m 4 -s 25 --send
successfully sent the SMS

mmcli -m 4 --3gpp-ussd-initiate="*101#"

	
 */
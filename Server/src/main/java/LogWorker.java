import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Exchanger;

/**
 * Ведет логирование действий сервера в файл.
 */
public class LogWorker implements Runnable {
    private String criticalError; //Сообщение об ошибке.
    private String filePath; //Путь к файлу для текущей сесси.
    Exchanger<String> exchanger; //Объект для обмена данными.
    private boolean stopThread; //Флаг остановки потока.

    public LogWorker() {
        stopThread=false; //Поток не нужно останавливать.
    }

    public String getCriticalError() {
        return criticalError;
    }

    public void stopThread() {
        this.stopThread = true;
    }

    /**
     * Создает и открывает файл для логирования данных по указанному пути.
     *
     * @param folderPath
     * @return
     */
    public boolean init(String folderPath) {
        try {
            //Создаем папку,если ее нет.
            File Dir = new File(folderPath);
            if (!Dir.exists()) {
                Dir.mkdir();
            }

            //Текущая дата.
            String timeStamp = new SimpleDateFormat("dd_MM_yyyy").format(Calendar.getInstance().getTime());
            filePath = folderPath + "/LOG[" + timeStamp + "].txt";

            //fos.write(buffer, 0, buffer.length);
        } catch (Exception ex) {

            System.out.println(ex.getMessage());
        }
        return true;
    }

    /**
     * Пишет строку в лог.
     *
     * @return
     */
    public boolean writeln(String text) {
        //Объект для записи файлов.
        try (FileOutputStream logFile = new FileOutputStream(filePath, true)) {
            String timeStamp = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(Calendar.getInstance().getTime());
            text = timeStamp + " " + text + "\r\n";
            logFile.write(text.getBytes(Charset.forName("UTF-8")));
            logFile.close();
        } catch (IOException ex) {

            System.out.println(ex.getMessage());
        }
        return true;
    }

    /**
     * Передает объект для обмена данными.
     *
     * @param exchanger
     */
    public void setExchanger(Exchanger<String> exchanger) {
        this.exchanger = exchanger;
    }

    public void run() {

        try {

            while (!Thread.currentThread().isInterrupted()) {

                String massage = exchanger.exchange(null);
                if (massage == null) continue;
                System.out.println(massage);
                writeln(massage);
            }
        } catch (Exception ex) {

        }

        System.out.println("Поток ведения логов остановлен.");

    }


}

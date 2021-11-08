package deviceOperator;

import java.io.*;
import java.util.List;

/**
 * Объект для работы с коммандной строкой linux.
 */
public class ShellExecutor {
    private String lastError;
    private String out;

    /**
     * Возврашает последнее сообщение об ошибке в классе.
     *
     * @return
     */
    public String getLastError() {
        return lastError;
    }

    /**
     * Возвращает вывод консоли.
     *
     * @return
     */
    public String getOut() {
        return out;
    }

    /**
     * Выполняет команду в коммандной строке.
     *
     * @param command
     * @return
     */
    public boolean executeShellCommand(String command) {

        StringBuffer output = new StringBuffer();

        Process p;
        try {
            p = Runtime.getRuntime().exec(command);
            p.waitFor(); //Ждем пока команда выполниться.
            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(p.getInputStream()));


            /*
              Process process = runtime.exec(args);
       InputStream is = process.getInputStream();
       InputStreamReader isr = new InputStreamReader(is);
       BufferedReader br = new BufferedReader(isr);
       String line;

       System.out.printf("Output of running %s is:",
           Arrays.toString(args));

       while ((line = br.readLine()) != null) {
         System.out.println(line);
       }
             */

            String line = "";
            while ((line = reader.readLine()) != null) {
                output.append(line + "\n");
            }
            out = output.toString();

        } catch (Exception e) {
            e.printStackTrace();
            lastError = e.getMessage();
        }

        return true;
    }


    /**
     * Выполняет команду в среде bash, нужно для работы из потока, если команда выполняется не в основном потоке, тогда команда выполняется без bash
     * и получаем ошибку.
     *
     * @param command
     * @return
     */
    public boolean executeBashCommand(String command) {
        StringBuffer output = new StringBuffer();

        Process process = null;
        try {
            //p = Runtime.getRuntime().exec(command);
            ProcessBuilder processBuilder = new ProcessBuilder(new String[]{"bash", "-l", "-c", command});
            process = processBuilder.start();

            //Входящий поток.
            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(process.getInputStream()));

            BufferedReader errorStream =
                    new BufferedReader(new InputStreamReader(process.getErrorStream()));

            process.waitFor();

            String line = "";
            while ((line = reader.readLine()) != null) {
                output.append(line + "\n");
            }
            out = output.toString();

            //Проверяю нет ли ошибки.
            while ((line = errorStream.readLine()) != null) {
                lastError=line;
                return false;
            }

        } catch (Exception e) {
            e.printStackTrace();
            lastError = e.getMessage();
        }

        return true;
    }


    public static void setUpStreamGobbler(final InputStream is, final PrintStream ps) {
        final InputStreamReader streamReader = new InputStreamReader(is);
        new Thread(new Runnable() {
            public void run() {
                BufferedReader br = new BufferedReader(streamReader);
                String line = null;
                try {
                    while ((line = br.readLine()) != null) {
                        ps.println("process stream: " + line);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        br.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    /*
     public static void main(String[] args) {


     */
    //Пример правильного исполнения. Удалить после полного теста
    public boolean executeShellCommand1(String command) {

        Process process = null;
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            process = processBuilder.start();
            InputStream inputStream = process.getInputStream();
            setUpStreamGobbler(inputStream, System.out);

            InputStream errorStream = process.getErrorStream();
            setUpStreamGobbler(errorStream, System.err);

            System.out.println("never returns");
            process.waitFor();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }


        /*

        StringBuffer output = new StringBuffer();

        Process process = null;
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(new String[]{"bash", "-l", "-c", command});
            process = processBuilder.start();

            //Входящий поток.
            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(process.getInputStream()));

            BufferedReader errorStream =
                    new BufferedReader(new InputStreamReader(process.getErrorStream()));

            process.waitFor();

            String line = "";
            while ((line = reader.readLine()) != null) {
                output.append(line + "\n");
            }
            out = output.toString();

            //Проверяю нет ли ошибки.
            while ((line = errorStream.readLine()) != null) {
                lastError=line;
                return false;
            }

        } catch (Exception e) {
            e.printStackTrace();
            lastError = e.getMessage();
        }
*/
        return true;
    }

    /**
     *     ProcessBuilder processBuilder = new ProcessBuilder(CMD_ARRAY);
     *          process = processBuilder.start();
     *          InputStream inputStream = process.getInputStream();
     *          setUpStreamGobbler(inputStream, System.out);
     *
     *          InputStream errorStream = process.getErrorStream();
     *          setUpStreamGobbler(errorStream, System.err);
     *
     *          System.out.println("never returns");
     *          process.waitFor();
     */


    /**
     * Преобразовывает ответ mmcli -L в список.
     *
     * @param modemNum
     * @return
     */
    public boolean parceModemNum(List<Integer> modemNum) {
        if ((out == null) || (out.isEmpty())) return true; //Пустой ответ.

        String[] parsed = out.split("\n");

        //Ищу номера модемов.
        int pos = 0;
        for (int i = 0; i < parsed.length; i++) {
            //Пример ответа org/freedesktop/ModemManager1/Modem/3 [huawei] E153.
            String str0 = parsed[i];
            str0 = str0.replace("ModemManager", "");
            str0 = str0.replace("modems", ""); //Для CentOs


            //Ответ содержит Modem.
            if (str0.contains("Modem")) {
                pos = str0.indexOf("Modem");
                String str1 = str0.substring(pos + 6, str0.length()); //Скопировали Modem/3 [huawei] E153.
                pos = str1.indexOf(" ");
                str1 = str1.substring(0, pos);
                modemNum.add(Integer.parseInt(str1));
            }
        }

        return true;
    }


    //str0=str0.replace("ModemManager","");
    // --version
}

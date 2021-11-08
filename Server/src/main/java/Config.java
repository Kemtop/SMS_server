import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.FileSystem;
import org.apache.commons.configuration.tree.xpath.XPathExpressionEngine;

import java.io.File;


public class Config {

    private  final String CONF_NAME="server.config"; //Имя конфигурационного файла.

    private  String logFilePath; //Путь к папке где храниться лог файл.

    public String getLogFilePath() {
        return logFilePath;
    }

    /**
     * Читает конфигурационный файл.
     * @return
     */
    boolean read()
    {
        try {
            File configFile = new File(CONF_NAME);
            if (!configFile.exists() && !configFile.isDirectory() ) {
                System.out.println("Ошибка: Не найден конфигурационный файл "+CONF_NAME);
                return false;
            }

            XMLConfiguration config= new XMLConfiguration(configFile);
            config.setExpressionEngine(new XPathExpressionEngine());

            //Путь к папке где храниться лог файл.
            logFilePath= config.getString("LogfilePath");
            if (logFilePath==null||logFilePath.isEmpty())
            {
                System.out.println("Ошибка: Не найден блок logFilePath в конфигурационном файле "+CONF_NAME);
                return false;
            }

        }
        catch(Exception ex)
        {
            System.out.println("Ошибка: Возникло исключение во время загрузки конфигурационного файла"+ex.getMessage()) ;
        }

        return  true;
    }

}

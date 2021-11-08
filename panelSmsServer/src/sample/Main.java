package sample;

import SmsClient.SmsClient;
import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Control;
import javafx.scene.image.Image;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;



public class Main extends Application {
//https://habr.com/ru/post/78035/ для сервера
    @Override
    public void start(Stage primaryStage) throws Exception{

        //Объект для взаимодействия с сервером.
         SmsClient sms = new SmsClient();

        //Создание главной формы.
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource(
                        "sample.fxml"
                )
        );

        //Cцена.
        Stage stage = new Stage(StageStyle.DECORATED);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.getIcons().add(new Image("file:.\\img\\appIco.png"));
        stage.setScene(
                new Scene(loader.load())
        );


        Controller controller=loader.getController(); //Получаю контроллер формы.
        controller.setSms(sms); //Передаю объект для работы с смс сервером.

        stage.setResizable(false);
        stage.setTitle("Управление смс сервером");

        //"Событие" по закрытию формы.
        stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            public void handle(WindowEvent we) {
                System.out.println("Stop client objects...");
                sms.invalidate();
            }
        });

        stage.show();


        /*
         Стандартный шаблон IDEA, удалить в релизе.
        Parent root = FXMLLoader.load(getClass().getResource("sample.fxml"));
        primaryStage.setTitle("Управление смс сервером");
        primaryStage.setResizable(false);
        primaryStage.setScene(new Scene(root, 1000, 618));
        primaryStage.show();
        */
    }


    public static void main(String[] args) {
        launch(args);
    }
}

package SmsClient;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.CharsetUtil;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectWriter;
import sample.models.mDevTypes;
import sample.models.mDevicesInfo;


import java.nio.charset.Charset;
import java.util.List;

public class SmsClient {
    EventLoopGroup workerGroup; //Интерфейс клиента.
    Bootstrap ClBootstrap; //Класс упрощающий загрузку  канала для использования клиентом.

    private String lastError; //сообщение об ошибке в любом методе класса.
    private String serverHost; //Cмс сервер.
    private int serverPort = 3700; //Порт сервера.
    private String firstCommand; //Сообщение отправляемое клиентом после подключение к серверу.
    private String serverAnswer; //Ответ сервера.


    public SmsClient() {
        workerGroup = new NioEventLoopGroup();
        ClBootstrap = new Bootstrap(); //Класс упрощающий загрузку  канала для использования клиентом.
        ClBootstrap.group(workerGroup);
        ClBootstrap.channel(NioSocketChannel.class); //SocketChannel, который использует реализацию на основе селектора NIO.
        ClBootstrap.option(ChannelOption.SO_KEEPALIVE, true);  //TCP Keepalive

        //ChannelHandler, который будет использоваться для обслуживания запросов.
        ClBootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel ch) throws Exception {
                //Добавляем в конец конвеера обработчик событий.
                ch.pipeline().addLast(new PacketFrameDecoder()).addLast(new ClientHandler());
            }
        });
    }


    /**
     * Получить ответ сервера.
     *
     * @return
     */
    public String getServerAnswer() {
        return serverAnswer;
    }


    /**
     * Возвращает сообщение об ошибке в любом методе класса.
     *
     * @return
     */
    public String getLastError() {
        return lastError;
    }

    public void setServerHost(String serverHost) {
        this.serverHost = serverHost;
    }

    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }

    /**
     * Отправляет сообщение.
     *
     * @param channel     канал
     * @param phoneNumber номер телефона
     * @param message     сообщение
     * @return
     */
    public boolean sendSms(String channel, String phoneNumber, String message) {
        try {
            firstCommand = "sendSms;" + channel + ";" + phoneNumber + ";" + message + '\n'; //Отправляемая команда серверу.

            // Ожидает этого будущего, пока оно не будет сделано, и повторно бросает причину неудачи, если это будущее не удалось.
            ChannelFuture f = ClBootstrap.connect(serverHost, serverPort).sync();
            //Ждем ответа сервера и закрытия соединения.
            f.channel().closeFuture().sync();

            //Сервер вернул ошибку.
            if (!serverAnswer.equals("OK\n")) {
                lastError = serverAnswer;
                return false;
            }

        } catch (Exception ex) {
            lastError = ex.getMessage();

            lastError = lastError.replace("Connection refused: no further information:", "В соединении отказано. Возможно сервер не существует.");

            return false;
        } finally {
            //workerGroup.shutdownGracefully();
        }
        return true;
    }

    /**
     * Получение свединий об устройствах на сервере.
     *
     * @return
     */
    public boolean getDeviceInfo() {
        try {
            firstCommand = "getDevInfo;\n"; //Отправляемая команда серверу.
            //System.out.println("First massage=" + firstCommand);

            // Ожидает этого будущего, пока оно не будет сделано, и повторно бросает причину неудачи, если это будущее не удалось.
            ChannelFuture f = ClBootstrap.connect(serverHost, serverPort).sync();
            //Ждем ответа сервера и закрытия соединения.
            f.channel().closeFuture().sync();

            //Сервер вернул ошибку?.
            String str = serverAnswer.substring(0, 2); //Первые два символа 'OK'.
            if (!str.equals("OK")) {
                lastError = serverAnswer;
                return false;
            }

            str = serverAnswer.substring(2, serverAnswer.length()); //Вырезаем нужную нам информацию.
            serverAnswer = str;


        } catch (Exception ex) {
            lastError = ex.getMessage();

            lastError = lastError.replace("Connection refused: no further information:", "В соединении отказано. Возможно сервер не существует.");

            return false;
        } finally {
            //workerGroup.shutdownGracefully();
        }
        return true;
    }

    /**
     * Возвращает модель настроек устройств полученную от сервера.
     */
    public mDevicesInfo getDevicesInfo() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            mDevicesInfo mData = mapper.readValue(serverAnswer, mDevicesInfo.class);
            return mData;

        } catch (Exception e) {
            lastError = e.getMessage();
            return null;
        }
    }


    /**
     * @param modelCfg
     * @return
     */
    public boolean setDevConfig(mDevicesInfo modelCfg) {
        try {
            ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();

            String json = ow.writeValueAsString(modelCfg);
            firstCommand = "setDevConfig;" + json + ";\n"; //Отправляемая команда серверу.

            // Ожидает этого будущего, пока оно не будет сделано, и повторно бросает причину неудачи, если это будущее не удалось.
            ChannelFuture f = ClBootstrap.connect(serverHost, serverPort).sync();
            //Ждем ответа сервера и закрытия соединения.
            f.channel().closeFuture().sync();

            //Сервер вернул ошибку.
            if (!serverAnswer.equals("OK\n")) {
                lastError = serverAnswer;
                return false;
            }


        } catch (Exception ex) {
            lastError = ex.getMessage();

            lastError = lastError.replace("Connection refused: no further information:", "В соединении отказано. Возможно сервер не существует.");

            return false;
        } finally {
            //workerGroup.shutdownGracefully();
        }
        return true;
    }


    /**
     * Получить типы устройств.
     *
     * @return
     */
    public mDevTypes getDevTypes() {
        try {
            firstCommand = "getDevTypes;\n"; //Отправляемая команда серверу.

            // Ожидает этого будущего, пока оно не будет сделано, и повторно бросает причину неудачи, если это будущее не удалось.
            ChannelFuture f = ClBootstrap.connect(serverHost, serverPort).sync();
            //Ждем ответа сервера и закрытия соединения.
            f.channel().closeFuture().sync();

            //Сервер вернул ошибку?.
            String str = serverAnswer.substring(0, 2); //Первые два символа 'OK'.
            if (!str.equals("OK")) {
                lastError = serverAnswer;
                return null;
            }

            str = serverAnswer.substring(2, serverAnswer.length()); //Вырезаем нужную нам информацию.
            serverAnswer = str;

            //Преобразуем json в модель.
            ObjectMapper mapper = new ObjectMapper();
            mDevTypes data = mapper.readValue(serverAnswer, mDevTypes.class);
            return data;

        } catch (Exception ex) {
            lastError = ex.getMessage();
            lastError = lastError.replace("Connection refused: no further information:", "В соединении отказано. Возможно сервер не существует.");
            return null;
        }

    }


    /**
     * Принять таблицу типов устройств.
     *
     * @param modelCfg
     * @return
     */
    public boolean setDevTypes(mDevTypes modelCfg) {
        try {
            ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();

            String json = ow.writeValueAsString(modelCfg);
            firstCommand = "setDevTypes;" + json + ";\n"; //Отправляемая команда серверу.

            // Ожидает этого будущего, пока оно не будет сделано, и повторно бросает причину неудачи, если это будущее не удалось.
            ChannelFuture f = ClBootstrap.connect(serverHost, serverPort).sync();
            //Ждем ответа сервера и закрытия соединения.
            f.channel().closeFuture().sync();

            //Сервер вернул ошибку.
            if (!serverAnswer.equals("OK\n")) {
                lastError = serverAnswer;
                return false;
            }


        } catch (Exception ex) {
            lastError = ex.getMessage();
            lastError = lastError.replace("Connection refused: no further information:", "В соединении отказано. Возможно сервер не существует.");

            return false;
        }
        return true;
    }


    /**
     * Возвращает настройки сервера. Только публичные данные в Т.З. Без данных устройств.
     *
     * @return
     */
    public mDevicesInfo getSrvSettings() {
        try {
            firstCommand = "getPublicParam;\n"; //Отправляемая команда серверу.

            // Ожидает этого будущего, пока оно не будет сделано, и повторно бросает причину неудачи, если это будущее не удалось.
            ChannelFuture f = ClBootstrap.connect(serverHost, serverPort).sync();
            //Ждем ответа сервера и закрытия соединения.
            f.channel().closeFuture().sync();

            //Сервер вернул ошибку?.
            String str = serverAnswer.substring(0, 2); //Первые два символа 'OK'.
            if (!str.equals("OK")) {
                lastError = serverAnswer;
                return null;
            }

            str = serverAnswer.substring(2, serverAnswer.length()); //Вырезаем нужную нам информацию.

            ObjectMapper mapper = new ObjectMapper();
            mDevicesInfo mData = mapper.readValue(str, mDevicesInfo.class);
            return mData;

        } catch (Exception ex) {
            lastError = ex.getMessage();
            lastError = lastError.replace("Connection refused: no further information:", "В соединении отказано. Возможно сервер не существует.");
            return null;
        }

    }


    /**
     * * Задает настройки сервера. Только публичные данные в Т.З. Без данных устройств.
     * @param modelCfg
     * @return
     */
    public boolean setSrvSettings(mDevicesInfo modelCfg) {
        try {

            ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();

            String json = ow.writeValueAsString(modelCfg);
            firstCommand = "setPublicParam;" + json + ";\n"; //Отправляемая команда серверу.

            // Ожидает этого будущего, пока оно не будет сделано, и повторно бросает причину неудачи, если это будущее не удалось.
            ChannelFuture f = ClBootstrap.connect(serverHost, serverPort).sync();
            //Ждем ответа сервера и закрытия соединения.
            f.channel().closeFuture().sync();

            //Сервер вернул ошибку.
            if (!serverAnswer.equals("OK\n")) {
                lastError = serverAnswer;
                return false;
            }


        } catch (Exception ex) {
            lastError = ex.getMessage();

            lastError = lastError.replace("Connection refused: no further information:", "В соединении отказано. Возможно сервер не существует.");

            return false;
        }
        return true;
    }


    /**
     * Анулирует поток клиента. Обязательно вызывать после завершении работы с объектом.
     */
    public void invalidate() {
        workerGroup.shutdownGracefully();
    }


    /**
     * Обработчик.
     */
    public class ClientHandler extends ChannelInboundHandlerAdapter {

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            //Сообщение отправляемое клиентом после подключение к серверу.
            ctx.writeAndFlush(Unpooled.copiedBuffer(firstCommand, CharsetUtil.UTF_8));
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            ByteBuf m = (ByteBuf) msg;
            try {
                serverAnswer = m.toString(Charset.forName("utf-8")); //m.readCharSequence(length, Charset.forName("utf-8")).toString();//  m.toString();

            } catch (Exception ex) {

            } finally {
                m.release();
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            cause.printStackTrace();
            ctx.close();
        }
    }

}

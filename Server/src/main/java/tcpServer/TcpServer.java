package tcpServer;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.handler.codec.MessageToMessageEncoder;
import tcpServer.CommandQueue;
import tcpServer.mQueueCommand;

/**
 * Сервер обрабатывающий сетевые пакеты от клиента.
 */
public class TcpServer {
    //Так называемая группа событий, используемая при создании каналов между серверами и клиентом
    EventLoopGroup bossGroup;

    private int port;
    private ArrayList<String> CMD=new ArrayList<String>();
    private CommandQueue cmdQueue; //Очередь команд.

    public TcpServer(int port) {
        this.port = port;
        cmdQueue=new CommandQueue();
    }


    public void run() throws Exception {

        //ExecutorService bossExec = new OrderedMemoryAwareThreadPoolExecutor(1, 400000000, 2000000000, 60, TimeUnit.SECONDS);
        //ExecutorService ioExec = new OrderedMemoryAwareThreadPoolExecutor(4 /* число рабочих потоков */, 400000000, 2000000000, 60, TimeUnit.SECONDS);
        // ServerBootstrap networkServer = new ServerBootstrap(new NioServerSocketChannelFactory(bossExec, ioExec,  4 /* то же самое число рабочих потоков */));
        // networkServer.setOption("backlog", 500);
        // networkServer.setOption("connectTimeoutMillis", 10000);
        // networkServer.setPipelineFactory(new ServerPipelineFactory());
        // Channel channel = networkServer.bind(new InetSocketAddress(address, port));


        //Так называемая группа событий, используемая при создании каналов между серверами и клиентом
        bossGroup = new NioEventLoopGroup(1);
        try {
            ServerBootstrap networkServer = new ServerBootstrap(); // Серверная петля, смотри видео по netty.

            networkServer.group(bossGroup)
                    .channel(NioServerSocketChannel.class) //Добавляем экземпляр канала.
                    .childHandler(new ChannelInitializer<SocketChannel>() { //Установка ChannelHandler, который используется для обслуживания запроса для Channel.

                        /*
                          Так как будут обрабатываться подключения и пакеты клиента, определяем конвеер(Pipeline),
                           который при открытии канала с клиентом создаёт для него pipeline,
                            в котором определены обработчики событий, которые происходят на канале. В нашем случае, это ServerPipelineFactory:
                         */
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline()
                                    //Собираем все байты во входном буфере, пока не прийдет байт с кодом 10(\n).
                                    // После этого пропускаем байты из входного буфера дальше по конвееру.
                                    .addLast(new tcpServer.PacketFrameDecoder())
                                    .addLast(new tcpServerHandler(cmdQueue));

                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)  //Максимальная длина очереди для входящих указаний на подключение.
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000)
                    .childOption(ChannelOption.SO_KEEPALIVE, true); //KeepAlive для tcp.

            // Bind and start to accept incoming connections.
            ChannelFuture f = networkServer.bind(port).sync(); // (7)

            // Wait until the server socket is closed.
            // In this example, this does not happen, but you can do that to gracefully
            // shut down your server.
            f.channel().closeFuture().sync();
        } catch (Exception ex)
        {
        }
        finally {
            //workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }

    /**
     * Остановка TCP сервера.
     */
    public void stop() {
        try {
            System.out.println("Остановка TCP сервера");
           //shutdown EventLoopGroup
            bossGroup.shutdownGracefully().sync();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * //Возвращает элемент из начала очереди TCP сервера.
     * @return
     */
    public mQueueCommand getRequest()
    {
        return  cmdQueue.getRequest();
    }

    /**
     * Отправляет в соединение ошибку.
     * @param ctx соединение
     * @param str
     */
    public static void returnError(ChannelHandlerContext ctx, String str)
    {
        ByteBuf data1= Unpooled.copiedBuffer(str+"\n", Charset.forName("utf-8"));
        /*
         Метод writeAndFlush асинхронный,
         однако он возвращает ChannelFuture, который позволяет добавить слушателя,
         который будет уведомлен, когда операция будет завершена.
         Закрываем соедиение только после того, как данные были записаны.
         */

        ctx.writeAndFlush(data1)
                .addListener(ChannelFutureListener.CLOSE);

    }

    public static void returnString(ChannelHandlerContext ctx, String str)
    {
        ByteBuf data1= Unpooled.copiedBuffer(str+"\n", Charset.forName("utf-8"));
        /*
         Метод writeAndFlush асинхронный,
         однако он возвращает ChannelFuture, который позволяет добавить слушателя,
         который будет уведомлен, когда операция будет завершена.
         Закрываем соедиение только после того, как данные были записаны.
         */

        ctx.writeAndFlush(data1)
                .addListener(ChannelFutureListener.CLOSE);

    }


    /**
     * Возвращает клиенту ОК, закрывает соединение.
     * @param ctx
     */
    public static void returnOK(ChannelHandlerContext ctx)
    {
        ByteBuf data1= Unpooled.copiedBuffer("OK\n", Charset.forName("utf-8"));
        /*
         Метод writeAndFlush асинхронный,
         однако он возвращает ChannelFuture, который позволяет добавить слушателя,
         который будет уведомлен, когда операция будет завершена.
         Закрываем соедиение только после того, как данные были записаны.
         */

        ctx.writeAndFlush(data1)
                .addListener(ChannelFutureListener.CLOSE);

    }


    /**
     * Возвращает клиенту строку содержащую json.
     * @param ctx
     */
    public static void returnJsonStr(ChannelHandlerContext ctx,String str)
    {
        String str1=str.replace('\n',' '); //Удалить симовлы так как /n управляющий.
        ByteBuf data1= Unpooled.copiedBuffer("OK"+str1+"\n", Charset.forName("utf-8"));
        /*
         Метод writeAndFlush асинхронный,
         однако он возвращает ChannelFuture, который позволяет добавить слушателя,
         который будет уведомлен, когда операция будет завершена.
         Закрываем соедиение только после того, как данные были записаны.
         */

        ctx.writeAndFlush(data1)
                .addListener(ChannelFutureListener.CLOSE);

    }



    /*
    Отправляет в соединение строку.
    public static void returnString(ChannelHandlerContext ctx, String str)
    {
        ByteBuf data1= Unpooled.copiedBuffer(str, Charset.forName("utf-8"));
        ctx.writeAndFlush(data1);
    }
     */




    public  class tcpServerHandler extends ChannelInboundHandlerAdapter
    {
        private CommandQueue cmdQueue; //Очередь команд.

        public tcpServerHandler(CommandQueue cmd) {
            cmdQueue=cmd;
        }

        @Override
        public  void  channelActive(ChannelHandlerContext ctx)
        {

        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            ByteBuf data = (ByteBuf) msg;

            String command=data.toString(Charset.forName("utf-8"));
            mQueueCommand m=new mQueueCommand();
            m.setCommand(command);
            m.setCtx(ctx);
           if(!cmdQueue.addCommand(m)) //Не смогли добавить команду в очередь или другие проблеммы.
           {
               //Строку в массив.
               ByteBuf data1= Unpooled.copiedBuffer(cmdQueue.getLastError(),Charset.forName("utf-8"));
               ctx.writeAndFlush(data1);
               //ctx.writeAndFlush(resultBuf).addListener(ChannelFutureListener.CLOSE);
               //ctx.write(a); //
               //ctx.flush(); //
           }


        }



        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) { // (4)
            // Close the connection when an exception is raised.
            cause.printStackTrace();
            ctx.close();
        }
    }
}




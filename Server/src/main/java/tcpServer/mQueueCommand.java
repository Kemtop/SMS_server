package tcpServer;

import io.netty.channel.ChannelHandlerContext;

/**
 * Модель блока данных очереди команд.
 */
public class mQueueCommand {
     /**
     * Команда принятая сервером.
     */
    private String command;
    /**
     * Объект из канала текущего соединения с клиентом.Используется для отправки ответа клиенту.
     */
    private ChannelHandlerContext ctx;

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public ChannelHandlerContext getCtx() {
        return ctx;
    }

    public void setCtx(ChannelHandlerContext ctx) {
        this.ctx = ctx;
    }
}

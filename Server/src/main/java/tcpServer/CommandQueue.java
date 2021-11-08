package tcpServer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Реализует логику очереди комманд которые приходят от клиентов к серверу.
 */
public class CommandQueue {
    int queueSize; //Размер очереди для быстрого анализа.
    Queue<mQueueCommand> queue; //Сама очередь команд.
    String lastError;

    public CommandQueue() {
        queueSize = 0;
        queue = new LinkedList<>();
    }

    /**
     * Получает последнее сообщение об ошибке.
     *
     * @return
     */
    public String getLastError() {
        return lastError;
    }

    /**
     * Добавляет команду в очередь.
     *
     * @param cmd
     */
    public boolean addCommand(mQueueCommand cmd) {
        //lastError = "OverFlow";
        //lastError = "Очередь перегружена";
        //return false;
        return queue.offer(cmd);

    }


    /**
     * Возвращает с удалением элемент из начала очереди TCP сервера. Если очередь пуста, возвращает значение null.
     * @return
     */
    public mQueueCommand getRequest()
    {
        return  queue.poll();
    }

    public String getCommand(String c)
    {
        mQueueCommand el=queue.element();

        ByteBuf data1= Unpooled.copiedBuffer(c, Charset.forName("utf-8"));
        el.getCtx().writeAndFlush(data1);

        return el.getCommand();
    }

}

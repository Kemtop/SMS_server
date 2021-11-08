package tcpServer;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.nio.charset.Charset;
import java.util.List;

/**
 * Класс решающий проблемму фрагментирования при приеме данных от клиента.
 * Т.е. если байты приходят кусками. Собирает весь пакет, пока он польностью не прийдет.
 */
public class PacketFrameDecoder extends ByteToMessageDecoder {

    //Собираем все байты во входном буфере, пока не прийдет байт с кодом 10(\n).
    // После этого пропускаем байты из входного буфера дальше по конвееру.
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {

        //В буфере менее 5 символов, такой команды не существует выходим.
        if (in.readableBytes() < 5) {
            return;
        }

        //out.add(in.readBytes(4)); // (4)

        in.markReaderIndex(); //Отмечает текущий readerIndex в этом буфере.
        if (!in.isReadable()) {
            in.resetReaderIndex();
            return;
        }

        int len=in.readableBytes();

        //Решить проблемму рус символов
        byte b = in.getByte(len-1); //Получаю последний байт в пакете.

        //System.out.println(in.toString(Charset.forName("utf-8")));
       //System.out.println(Integer.toString(len));
        //System.out.println(Byte.toString(b));

        //Не пришел байт завершения команды /n.
        if (b != 10) {
            in.resetReaderIndex();
            return;
        }

        out.add(in.readBytes(in.readableBytes()));


    }
}

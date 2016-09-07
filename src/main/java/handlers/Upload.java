package handlers;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.router.Routed;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;


/**
 * Created by Baka on 05.12.2015.
 */
public class Upload extends SimpleChannelInboundHandler<Routed> {

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Routed routed) throws Exception {

        HttpObject msg = (HttpObject) routed.request();

        if (msg instanceof HttpContent) {

            HttpContent chunk = (HttpContent) msg;
            System.out.println(chunk.content().toString());

           ByteBuf buf = chunk.content();
            byte[] bytes;
            int offeset;
            int length = buf.readableBytes();

            if (buf.hasArray()){
                bytes = buf.array();
                offeset = buf.arrayOffset();
            }else {
                bytes = new byte[length];
                buf.getBytes(buf.readerIndex(), bytes);
                offeset = 0;
            }

            BufferedImage imag = ImageIO.read(new ByteArrayInputStream(bytes));
            ImageIO.write(imag, "jpg", new File("C:\\recobest\\","snap.jpg"));
        }

    }


}

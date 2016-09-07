package handlers.picmanager;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.router.Routed;
import redis.RedisManager;
import redis.clients.jedis.Jedis;
import utils.SendResponse;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;

/**
 * Created by Baka on 04.10.2015.
 */
@ChannelHandler.Sharable
public class DownloadPicture extends SimpleChannelInboundHandler<Routed> {
    BufferedImage bufferedImage;
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Routed routed) throws Exception {
        String params = routed.request().getUri();
        QueryStringDecoder queryStringDecoder = new QueryStringDecoder(params);
        String uid = queryStringDecoder.parameters().get("uid").get(0);

        SendResponse.flushImg(routed.request(), channelHandlerContext, imgInByte(uid));

    }

    public byte[] imgInByte (String uid){
        Jedis conn = null;
        String imgName = "";
        byte[] imgToSend = new byte[10000];

        try{
            conn = RedisManager.pool.getResource();
            imgName = conn.hget("user:" + uid, "profilePic");
        }finally {
            if (conn != null){
                conn.close();
            }
        }

        try{

            bufferedImage = ImageIO.read(new File("C:/recobest/img/" + imgName + ".jpg"));
            ImageIO.write(bufferedImage, "jpg", byteArrayOutputStream);
            byteArrayOutputStream.flush();
            imgToSend = byteArrayOutputStream.toByteArray();
            byteArrayOutputStream.close();

        }catch (Exception e){
            e.printStackTrace();
        }

        return imgToSend;

    }
}

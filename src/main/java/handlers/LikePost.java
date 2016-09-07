package handlers;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.router.Routed;
import models.Like;
import redis.RedisManager;
import redis.clients.jedis.Jedis;
import utils.JsonHandler;
import utils.PostBody;
import utils.SendResponse;

/**
 * Created by Baka on 26.09.2015.
 */
@ChannelHandler.Sharable
public class LikePost extends SimpleChannelInboundHandler<Routed> {

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Routed routed) throws Exception {
        String input = PostBody.getPostBody(routed.request());
        Like likeObj = (Like) JsonHandler.toJava(input, "models.Like");

        Jedis conn = null;
        try{
            conn = RedisManager.pool.getResource();
            long res = RedisManager.likePost(conn, likeObj.getPid(), likeObj.getUid());
            SendResponse.flush(routed.request(), null, channelHandlerContext, String.valueOf(res));
        }finally {
            if (conn != null){
                conn.close();
            }
        }

    }
}

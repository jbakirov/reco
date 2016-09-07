package handlers;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.router.Routed;
import redis.RedisManager;
import redis.clients.jedis.Jedis;
import utils.SendResponse;

/**
 * Created by Baka on 28.09.2015.
 */
@ChannelHandler.Sharable
public class DeletePost extends SimpleChannelInboundHandler<Routed> {

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Routed routed) throws Exception {
        String params = routed.request().getUri();
        QueryStringDecoder queryStringDecoder = new QueryStringDecoder(params);
        String uid = queryStringDecoder.parameters().get("uid").get(0);
        String postId = queryStringDecoder.parameters().get("pid").get(0);

        Jedis conn = null;
        try{
            conn = RedisManager.pool.getResource();
            boolean res = RedisManager.deletePost(conn, uid, postId);
            SendResponse.flush(routed.request(), null, channelHandlerContext, String.valueOf(res));
        }finally {
            if (conn != null){
                conn.close();
            }
        }
    }
}

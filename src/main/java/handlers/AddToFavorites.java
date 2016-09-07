package handlers;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.router.Routed;
import redis.RedisManager;
import redis.clients.jedis.Jedis;
import utils.JsonHandler;
import utils.SendResponse;

/**
 * Created by Baka on 18.11.2015.
 */
@ChannelHandler.Sharable
public class AddToFavorites extends SimpleChannelInboundHandler<Routed> {

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Routed routed) throws Exception {
        String params = routed.request().getUri();
        QueryStringDecoder queryStringDecoder = new QueryStringDecoder(params);
        String itemId = queryStringDecoder.parameters().get("itemId").get(0);
        String uid = queryStringDecoder.parameters().get("uid").get(0);
        String type = queryStringDecoder.parameters().get("type").get(0);

        Jedis conn = null;
        try{
            conn = RedisManager.pool.getResource();
            if (RedisManager.addToFavorites(conn, Long.parseLong(itemId), Long.parseLong(uid), type))
                SendResponse.flush(routed.request(), HttpResponseStatus.ACCEPTED, channelHandlerContext, null);
        }finally {
            if (conn != null){
                conn.close();
            }
        }
    }
}

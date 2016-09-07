package handlers;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.router.Routed;
import models.User;
import redis.RedisManager;
import redis.clients.jedis.Jedis;
import utils.JsonHandler;
import utils.SendResponse;

import java.util.List;

/**
 * Created by Baka on 25.09.2015.
 */

@ChannelHandler.Sharable
public class UserSearch extends SimpleChannelInboundHandler<Routed> {

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Routed routed) throws Exception {
        String params = routed.request().getUri();
        QueryStringDecoder queryStringDecoder = new QueryStringDecoder(params);
        String uid = queryStringDecoder.parameters().get("uid").get(0);
        String value = queryStringDecoder.parameters().get("value").get(0);

        Jedis conn = null;
        List<User> result = null;
        try{
            conn = RedisManager.pool.getResource();
            result = RedisManager.getUserSearch(conn, value, 10, Long.parseLong(uid));
        }finally {
            if (conn != null){
                conn.close();
            }
        }

        if (result != null){
            SendResponse.flush(routed.request(), null, channelHandlerContext, JsonHandler.toJson(result));
        }
    }
}

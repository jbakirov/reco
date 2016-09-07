package handlers;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.router.Routed;
import redis.RedisManager;
import redis.clients.jedis.Jedis;
import utils.JsonHandler;
import utils.SendResponse;

import java.util.List;
import java.util.Map;

/**
 * Created by Baka on 18.11.2015.
 */
public class GetFriendsQuestions extends SimpleChannelInboundHandler<Routed> {
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Routed routed) throws Exception {
        String params = routed.request().getUri();
        QueryStringDecoder queryStringDecoder = new QueryStringDecoder(params);
        String fid = queryStringDecoder.parameters().get("fid").get(0);
        String page = queryStringDecoder.parameters().get("page").get(0);

        Jedis conn = null;
        try{
            conn = RedisManager.pool.getResource();

            List<Map<String, String>> result = RedisManager.getFriendsQuestions(conn, Long.parseLong(fid), Integer.parseInt(page), 10);

            if (result != null){
                SendResponse.flush(routed.request(), null, channelHandlerContext, JsonHandler.toJson(result));
            }

        }finally {
            if (conn != null){
                conn.close();
            }
        }
    }
}

package handlers;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.router.Routed;
import models.FeedItem;
import redis.RedisManager;
import redis.clients.jedis.Jedis;
import utils.JsonHandler;
import utils.SendResponse;

import java.util.List;

/**
 * Created by Baka on 18.11.2015.
 */
@ChannelHandler.Sharable
public class GetUserFavoritePosts extends SimpleChannelInboundHandler<Routed> {

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Routed routed) throws Exception {
        String params = routed.request().getUri();
        QueryStringDecoder queryStringDecoder = new QueryStringDecoder(params);
        String page = queryStringDecoder.parameters().get("page").get(0);
        String uid = queryStringDecoder.parameters().get("uid").get(0);

        Jedis conn = null;
        try{
            conn = RedisManager.pool.getResource();
            List<FeedItem> result = RedisManager.getUserFavPosts(conn, Long.parseLong(uid), Integer.parseInt(page), 10);
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

package handlers;

import dbmanager.Queries;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.router.Routed;
import models.Item;
import redis.RedisManager;
import redis.clients.jedis.Jedis;
import utils.JsonHandler;
import utils.SendResponse;

import java.sql.Connection;
import java.util.List;
import java.util.Set;

/**
 * Created by Baka on 19.01.2016.
 */
@ChannelHandler.Sharable
public class GetTop10 extends SimpleChannelInboundHandler<Routed> {

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Routed routed) throws Exception {

        String params = routed.request().getUri();
        QueryStringDecoder queryStringDecoder = new QueryStringDecoder(params);
        String type = queryStringDecoder.parameters().get("type").get(0);

        Jedis conn = null;
        try{
            conn = RedisManager.pool.getResource();
            Set<String> top10 = RedisManager.getTop10(type, conn);
            List<Item> itemList = Queries.getTop10Items(top10, type);

            SendResponse.flush(routed.request(), null, channelHandlerContext, JsonHandler.toJson(itemList));

        }finally {
            if (conn != null){
                conn.close();
            }
        }

    }
}

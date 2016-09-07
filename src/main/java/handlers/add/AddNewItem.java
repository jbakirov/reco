package handlers.add;

import dbmanager.Queries;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.router.Routed;
import models.Item;
import redis.RedisManager;
import redis.clients.jedis.Jedis;
import utils.JsonHandler;
import utils.PostBody;
import utils.SendResponse;

import java.sql.SQLException;


/**
 * Created by Baka on 01.10.2015.
 */
@ChannelHandler.Sharable
public class AddNewItem extends SimpleChannelInboundHandler<Routed> {

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Routed routed) throws Exception {
        String input = PostBody.getPostBody(routed.request());
        Item item = (Item) JsonHandler.toJava(input, "models.Item");
        Long res = null;
        switch (item.getLang()){
            case "en":
                res = addToRedis(item);
                break;
            case "ru":
                res = addToRedisRu(item);
                break;
        }

        SendResponse.flush(routed.request(), null, channelHandlerContext, res.toString());
    }

    private Long addToRedis (Item item) throws SQLException {

        Long newItemId = Queries.addNewItem(item);

        Jedis conn = null;
        try{
            conn = RedisManager.pool.getResource();
            switch (item.getType()){
                case "yvideo":
                    break;
                case "movie":
                    RedisManager.createNewItem(conn, String.valueOf(newItemId), item.getScreenShots(), item.getCitation(), item.getTrailerUrl(), item.getType());
                    break;
                case "tvshow":
                    RedisManager.createNewItem(conn, String.valueOf(newItemId), item.getScreenShots(), item.getCitation(), item.getTrailerUrl(), item.getType());
                    break;
            }

        }finally {
            if (conn != null){
                conn.close();
            }
        }

        return newItemId;
    }


    private Long addToRedisRu (Item item) throws SQLException {

        Long newItemId = Queries.addNewItemRu(item);

        Jedis conn = null;
        try{
            conn = RedisManager.pool.getResource();
            RedisManager.createNewItem(conn, String.valueOf(newItemId), item.getScreenShots(), item.getCitation(), item.getTrailerUrl(), item.getType());
        }finally {
            if (conn != null){
                conn.close();
            }
        }
        return newItemId;
    }
}

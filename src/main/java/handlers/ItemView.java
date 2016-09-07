package handlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dbmanager.Queries;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.router.Routed;
import models.Item;
import redis.RedisManager;
import redis.clients.jedis.Jedis;
import utils.JsonHandler;
import utils.PostBody;
import utils.SendResponse;

import java.io.StringReader;
import java.lang.reflect.Field;

/**
 * Created by Baka on 10.10.2015.
 */
@ChannelHandler.Sharable
public class ItemView extends SimpleChannelInboundHandler<Routed> {

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Routed routed) throws Exception {

        String input = PostBody.getPostBody(routed.request());
        JsonNode node = new ObjectMapper().readTree(new StringReader(input));
        String itemid = node.get("item_id").asText();
        String type = node.get("type").asText();

        Jedis conn = null;
        try{
            conn = RedisManager.pool.getResource();
            Item item = (Item) RedisManager.getFullItemView(conn, itemid, type);
            Item item1 = (Item) Queries.getItemById(itemid, type);

            Item itemFinal = mergeObjects(item, item1);
            itemFinal.setId(Long.parseLong(itemid));

            SendResponse.flush(routed.request(), null, channelHandlerContext, JsonHandler.toJson(itemFinal));
        }finally {
            if (conn != null){
                conn.close();
            }
        }

    }

    @SuppressWarnings("unchecked")
    public static <T> T mergeObjects(T first, T second) throws IllegalAccessException, InstantiationException {
        Class<?> clazz = first.getClass();
        Field[] fields = clazz.getDeclaredFields();
        Object returnValue = clazz.newInstance();
        for (Field field : fields) {
            field.setAccessible(true);
            Object value1 = field.get(first);
            Object value2 = field.get(second);
            Object value = (value1 != null) ? value1 : value2;
            field.set(returnValue, value);
        }
        return (T) returnValue;
    }
}

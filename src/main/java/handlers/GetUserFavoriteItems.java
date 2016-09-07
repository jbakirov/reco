package handlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import utils.PostBody;
import utils.SendResponse;

import java.io.StringReader;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by Baka on 22.03.2016.
 */
@ChannelHandler.Sharable
public class GetUserFavoriteItems extends SimpleChannelInboundHandler<Routed> {

    private String[] types = {"book", "music", "yvideo", "place", "tvshow", "movie"};
    private List<Item> items;
    private Set<String> favItemIds;

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Routed routed) throws Exception {

        String params = routed.request().getUri();
        QueryStringDecoder queryStringDecoder = new QueryStringDecoder(params);
        String page = queryStringDecoder.parameters().get("page").get(0);
        String uid = queryStringDecoder.parameters().get("uid").get(0);
        items = new ArrayList<>();

        Jedis conn = null;
        try{
            conn = RedisManager.pool.getResource();

            for (String s : types){
                favItemIds = RedisManager.getUserFavItems(conn, Long.parseLong(uid), Integer.parseInt(page), 5, s);

                for (String id: favItemIds){
                    Item item = (Item) RedisManager.getFullItemView(conn, id, s);
                    Item item1 = (Item) Queries.getItemById(id, s);

                    Item itemFinal = mergeObjects(item, item1);
                    itemFinal.setId(Long.parseLong(id));
                    itemFinal.setType(s);

                    items.add(itemFinal);
                }
            }

            SendResponse.flush(routed.request(), null, channelHandlerContext, JsonHandler.toJson(items));
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

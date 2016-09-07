package handlers;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.router.Routed;
import models.HomeFeed;
import redis.RedisManager;
import redis.clients.jedis.Jedis;
import utils.JsonHandler;
import utils.SendResponse;

import java.util.List;
import java.util.Map;

/**
 * Created by Baka on 04.09.2015.
 */
@ChannelHandler.Sharable
public class GetAllComments extends SimpleChannelInboundHandler<Routed> {

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Routed routed) throws Exception {
        String params = routed.request().getUri();
        QueryStringDecoder queryStringDecoder = new QueryStringDecoder(params);
        String uid = queryStringDecoder.parameters().get("uid").get(0);
        String postId = queryStringDecoder.parameters().get("pid").get(0);

        Jedis conn = null;
        try{
            conn = RedisManager.pool.getResource();
            if (!isValidQuery(routed.request(), conn, uid)){
                SendResponse.flush(routed.request(), HttpResponseStatus.BAD_REQUEST, channelHandlerContext, "");
            }else {
                List<Map<String, String>> comments = RedisManager.getComments(conn, Long.parseLong(postId), 0, -1, true);
                if (comments != null){
                    SendResponse.flush(routed.request(), null, channelHandlerContext, JsonHandler.toJson(comments));
                }
            }

        }finally {
            RedisManager.pool.returnResource(conn);
        }
    }

    public boolean isValidQuery(HttpRequest httpRequest, Jedis conn, String uid) {


        String token = httpRequest.headers().get("Token");
        String uidFromRedis = conn.get("usertoken:" + uid);

        if (uidFromRedis != null) {
            if (uidFromRedis.equals(token)){
                return true;
            }
        }
        return false;
    }
}

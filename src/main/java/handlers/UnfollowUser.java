package handlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.router.Routed;
import redis.RedisManager;
import redis.clients.jedis.Jedis;
import utils.PostBody;
import utils.SendResponse;

import java.io.StringReader;

/**
 * Created by Baka on 02.09.2015.
 */
@ChannelHandler.Sharable
public class UnfollowUser extends SimpleChannelInboundHandler<Routed> {

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Routed routed) throws Exception {
        String input = PostBody.getPostBody(routed.request());
        JsonNode node = new ObjectMapper().readTree(new StringReader(input));
        String u1 = node.get("user1").asText();
        String u2 = node.get("user2").asText();
        String result = "fail";
        Jedis conn = null;
        try {
            conn = RedisManager.pool.getResource();
            if (RedisManager.unfollowUser(conn, Long.parseLong(u1), Long.parseLong(u2)))
                result = "success";
        } finally {
            RedisManager.pool.returnResource(conn);
        }

        SendResponse.flush(routed.request(), null, channelHandlerContext, result);
    }

}
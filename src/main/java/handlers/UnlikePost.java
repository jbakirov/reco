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
 * Created by Baka on 28.09.2015.
 */
@ChannelHandler.Sharable
public class UnlikePost extends SimpleChannelInboundHandler<Routed> {
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Routed routed) throws Exception {
        String input = PostBody.getPostBody(routed.request());
        JsonNode node = new ObjectMapper().readTree(new StringReader(input));

        Jedis conn = null;
        try {
            conn = RedisManager.pool.getResource();
            boolean b = RedisManager.unlikePost(conn, node.get("pid").asText(), node.get("uid").asText());
            SendResponse.flush(routed.request(), null, channelHandlerContext, String.valueOf(b));
        } finally {
            if (conn != null) {
                conn.close();
            }
        }

    }
}

package handlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.router.Routed;
import io.netty.handler.timeout.ReadTimeoutException;
import redis.RedisManager;
import redis.clients.jedis.Jedis;
import utils.PostBody;
import utils.SendResponse;

import java.io.StringReader;

/**
 * Created by Baka on 28.09.2015.
 */
@ChannelHandler.Sharable
public class DeleteComment extends SimpleChannelInboundHandler<Routed> {
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Routed routed) throws Exception {
        String input = PostBody.getPostBody(routed.request());
        JsonNode node = new ObjectMapper().readTree(new StringReader(input));
        String uid = node.get("uid").asText();
        String pid = node.get("pid").asText();
        String cid = node.get("cid").asText();

        Jedis conn = null;
        try {
            conn = RedisManager.pool.getResource();
            boolean res  = RedisManager.deleteComment(conn, uid, pid, cid);
            SendResponse.flush(routed.request(), null, channelHandlerContext, String.valueOf(res));
        }finally {
            if (conn != null){
                conn.close();
            }
        }
    }
}

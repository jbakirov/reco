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
 * Created by Baka on 06.11.2015.
 */
@ChannelHandler.Sharable
public class AnswerToQuestion extends SimpleChannelInboundHandler<Routed> {

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Routed routed) throws Exception {
        String input = PostBody.getPostBody(routed.request());
        JsonNode node = new ObjectMapper().readTree(new StringReader(input));
        String uid = node.get("uid").asText();
        String qid = node.get("qid").asText();
        String answer = node.get("answer").asText();
        String username = node.get("uname").asText();

        Jedis conn = null;

        try{
            conn = RedisManager.pool.getResource();
            long r = RedisManager.answerQuestion(conn, Long.parseLong(qid), Long.parseLong(uid), answer, username);
            if (r > 0){
                SendResponse.flush(routed.request(), null, channelHandlerContext, "success");
            }

        }finally {
            if (conn != null){
                conn.close();
            }
        }

    }
}

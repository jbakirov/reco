package handlers;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.router.Routed;
import models.QuestionResponse;
import redis.RedisManager;
import redis.clients.jedis.Jedis;
import utils.JsonHandler;
import utils.SendResponse;

import java.util.List;
import java.util.Map;

/**
 * Created by Baka on 06.11.2015.
 */
@ChannelHandler.Sharable
public class GetFullQuestionWithAnswers extends SimpleChannelInboundHandler<Routed> {
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Routed routed) throws Exception {
        String params = routed.request().getUri();
        QueryStringDecoder queryStringDecoder = new QueryStringDecoder(params);
        String qid = queryStringDecoder.parameters().get("qid").get(0);
        String uid = queryStringDecoder.parameters().get("uid").get(0);

        Jedis conn = null;
        try{
            conn = RedisManager.pool.getResource();

            Map<String, String> question = RedisManager.getQuestionById(conn, Long.parseLong(qid), Long.parseLong(uid));
            List<Map<String, String>> answers = RedisManager.getAnswers(conn, Long.parseLong(qid), 1, 10);

            if (question != null || !question.isEmpty()){
                QuestionResponse questionResponse = new QuestionResponse();
                questionResponse.setQuestion(question);
                questionResponse.setComments(answers);

                SendResponse.flush(routed.request(), null, channelHandlerContext, JsonHandler.toJson(questionResponse));
            }

        }finally {
            if (conn != null){
                conn.close();
            }
        }
    }
}

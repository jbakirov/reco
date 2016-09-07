package handlers;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.router.Routed;
import models.Question;
import redis.RedisManager;
import redis.clients.jedis.Jedis;
import utils.JsonHandler;
import utils.PostBody;
import utils.SendResponse;

/**
 * Created by Baka on 03.09.2015.
 */
public class AskQuestion extends SimpleChannelInboundHandler<Routed> {

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Routed routed) throws Exception {

        String input = PostBody.getPostBody(routed.request());
        Question question = (Question) JsonHandler.toJava(input, "models.Question");

        String result = "success";
        Jedis conn = null;

        try{
            conn = RedisManager.pool.getResource();
            long r = RedisManager.askQuestion(conn, question.getUid(), question.getUsername(), question.getQuestion(), question.getTag());
            if (r <= 0){
                result = "fail";
            }
        }finally {
            if (conn != null){
                conn.close();
            }
        }
        SendResponse.flush(routed.request(), null, channelHandlerContext, result);
    }


}

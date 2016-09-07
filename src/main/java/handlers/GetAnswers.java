package handlers;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.router.Routed;
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
public class GetAnswers extends SimpleChannelInboundHandler<Routed> {

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Routed routed) throws Exception {
        String params = routed.request().getUri();
        QueryStringDecoder queryStringDecoder = new QueryStringDecoder(params);
        String qid = queryStringDecoder.parameters().get("qid").get(0);
        String page = queryStringDecoder.parameters().get("page").get(0);

        Jedis conn = null;
        try{
            conn = RedisManager.pool.getResource();
            if (Integer.parseInt(page) > 1){
                List<Map<String, String>> answers = RedisManager.getAnswers(conn, Long.parseLong(qid), Integer.parseInt(page), 10);
                if (!answers.isEmpty()){
                    SendResponse.flush(routed.request(), null, channelHandlerContext, JsonHandler.toJson(answers));
                }
            }



        }finally {
            if (conn != null){
                conn.close();
            }
        }
    }
}

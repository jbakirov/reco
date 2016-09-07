package handlers;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.router.Routed;
import models.Comment;
import models.HomeFeed;
import redis.RedisManager;
import redis.clients.jedis.Jedis;
import utils.JsonHandler;
import utils.PostBody;
import utils.SendResponse;

/**
 * Created by Baka on 04.09.2015.
 */
@ChannelHandler.Sharable
public class CommentPost extends SimpleChannelInboundHandler<Routed> {

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Routed routed) throws Exception {
        String input = PostBody.getPostBody(routed.request());
        Comment comment = (Comment) JsonHandler.toJava(input, "models.Comment");

        Jedis conn = null;
        try{
            conn = RedisManager.pool.getResource();
            if (!isValidQuery(routed.request(), conn, comment)) {
                SendResponse.flush(routed.request(), HttpResponseStatus.BAD_REQUEST, channelHandlerContext, "");
            }
            long res = RedisManager.createComment(conn, comment.getUid(), comment.getUsername(), String.valueOf(comment.getPostId()), comment.getComment());
            if (res > 0){
                SendResponse.flush(routed.request(), null, channelHandlerContext, String.valueOf(res));
            }else {
                SendResponse.flush(routed.request(), HttpResponseStatus.NOT_FOUND, channelHandlerContext, String.valueOf(""));
            }
        }finally {
            if (conn != null){
                conn.close();
            }
        }
    }

    public boolean isValidQuery(HttpRequest httpRequest, Jedis conn, Comment comment) {

        String token = httpRequest.headers().get("Token");
        String uid = String.valueOf(comment.getUid());
        String uidFromRedis = conn.get("usertoken:" + uid);


        if (uidFromRedis != null) {
            if (uidFromRedis.equals(token)){
                return true;
            }
        }
        return false;
    }

}

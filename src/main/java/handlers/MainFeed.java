package handlers;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.router.Routed;
import models.FeedItem;
import models.HomeFeed;
import redis.RedisManager;
import redis.clients.jedis.Jedis;
import utils.JsonHandler;
import utils.PostBody;
import utils.SendResponse;

import java.util.List;

/**
 * Created by Baka on 10.08.2015.
 */
@ChannelHandler.Sharable
public class MainFeed extends SimpleChannelInboundHandler<Routed> {

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Routed routed) throws Exception {

        String input = PostBody.getPostBody(routed.request());
        HomeFeed homeFeed = (HomeFeed) JsonHandler.toJava(input, "models.HomeFeed");

        Jedis conn = null;
        try {
            conn = RedisManager.pool.getResource();

            if (!isValidQuery(routed.request(), conn, homeFeed)) {
                SendResponse.flush(routed.request(), HttpResponseStatus.BAD_REQUEST,channelHandlerContext, "");
            } else {
                List<FeedItem> hFeed = RedisManager.getHomePosts(conn, homeFeed.getUid(), homeFeed.getPage(), 7);
                if (hFeed != null) {
                    SendResponse.flush(routed.request(), null, channelHandlerContext, JsonHandler.toJson(hFeed));
                }
            }

        } finally {
            if (conn != null){
                conn.close();
            }
        }
    }

    public boolean isValidQuery(HttpRequest httpRequest, Jedis conn, HomeFeed homeFeed) {


        String token = httpRequest.headers().get("Token");
        String uid = String.valueOf(homeFeed.getUid());
        String uidFromRedis = conn.get("usertoken:" + uid);


        if (uidFromRedis != null) {
            if (uidFromRedis.equals(token)){
                return true;
            }
        }
        return false;
    }

}

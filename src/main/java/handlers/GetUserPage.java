package handlers;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.router.Routed;
import models.HomeFeed;
import redis.RedisManager;
import redis.clients.jedis.Jedis;
import utils.JsonHandler;
import utils.PostBody;
import utils.SendResponse;

/**
 * Created by Baka on 27.09.2015.
 */
@ChannelHandler.Sharable
public class GetUserPage extends SimpleChannelInboundHandler<Routed>{

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Routed routed) throws Exception {

        String input = PostBody.getPostBody(routed.request());
        HomeFeed profileFeed = (HomeFeed) JsonHandler.toJava(input, "models.HomeFeed");

        Jedis conn = null;
        try {
            conn = RedisManager.pool.getResource();
            models.UserPage userPage = RedisManager.getUserPage(conn, String.valueOf(profileFeed.getUid()), String.valueOf(profileFeed.getRuid()), profileFeed.getPage());
            if (userPage != null) {
                SendResponse.flush(routed.request(), null, channelHandlerContext, JsonHandler.toJson(userPage));
            }
        } finally {
            if (conn != null) {
                conn.close();
            }
        }
    }
}

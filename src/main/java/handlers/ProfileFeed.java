package handlers;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
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
 * Created by Baka on 04.09.2015.
 */
@ChannelHandler.Sharable
public class ProfileFeed extends SimpleChannelInboundHandler<Routed> {

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Routed routed) throws Exception {
        String input = PostBody.getPostBody(routed.request());
        HomeFeed profileFeed = (HomeFeed) JsonHandler.toJava(input, "models.HomeFeed");

        Jedis conn = null;
        try {
            conn = RedisManager.pool.getResource();
            List<FeedItem> pFeed = RedisManager.getProfilePosts(conn, profileFeed.getUid(), profileFeed.getPage(), 7, profileFeed.getRuid());
            if (pFeed != null) {
                SendResponse.flush(routed.request(), null, channelHandlerContext, JsonHandler.toJson(pFeed));
            }
        } finally {
            if (conn != null) {
                conn.close();
            }
        }
    }
}

package handlers.picmanager;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.router.Routed;
import redis.RedisManager;
import redis.clients.jedis.Jedis;

/**
 * Created by Baka on 04.10.2015.
 */
@ChannelHandler.Sharable
public class UpdateProfilePicture extends SimpleChannelInboundHandler<Routed> {
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Routed routed) throws Exception {
        String params = routed.request().getUri();
        QueryStringDecoder queryStringDecoder = new QueryStringDecoder(params);
        String uid = queryStringDecoder.parameters().get("uid").get(0);

    }


}

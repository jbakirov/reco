package handlers;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.router.Routed;
import io.netty.handler.timeout.ReadTimeoutException;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;
import lucene.LuceneManager;
import utils.JsonHandler;
import utils.SendResponse;

import java.nio.charset.Charset;

import static io.netty.handler.codec.http.HttpHeaders.Names.*;

/**
 * Created by Baka on 02.09.2015.
 */
@ChannelHandler.Sharable
public class SearchItem extends SimpleChannelInboundHandler<Routed> {

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Routed routed) throws Exception {
        String params = routed.request().getUri();
        QueryStringDecoder queryStringDecoder = new QueryStringDecoder(params);
        String type = queryStringDecoder.parameters().get("type").get(0);
        String value = queryStringDecoder.parameters().get("value").get(0);
        LuceneManager l = new LuceneManager();
        String result = "";

        switch (type) {
            case "movie":
                break;
            case "book":
                result = l.searchBook(value);
                SendResponse.flush(routed.request(), null, channelHandlerContext, result);
                break;
            case "tvshow":
                break;
            case "place":
                break;
            case "yvideo":
                break;
            case "music":
                result = l.searchMusicFullText(value);
                SendResponse.flush(routed.request(), null, channelHandlerContext, result);
                break;
        }
    }

    private ByteBuf wrapUTF8String(String content) {
        return Unpooled.copiedBuffer(content.toCharArray(), CharsetUtil.UTF_8);
    }


    public void flush(HttpRequest request, ChannelHandlerContext channelHandlerContext, String token, String result) {
        boolean keepAlive = HttpHeaders.isKeepAlive(request);
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, wrapUTF8String(result).retain());
        response.headers().set(CONTENT_TYPE, "text/plain");
        response.headers().set(CONTENT_LENGTH, response.content().readableBytes());
        response.headers().set("Token", token);
        response.headers().add("Access-Control-Allow-Origin", "*");
        response.headers().add("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS");
        response.headers().add("Access-Control-Allow-Headers", "X-Requested-With, Content-Type, Content-Length");

        if (!keepAlive) {
            channelHandlerContext.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        } else {
            response.headers().set(CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
            channelHandlerContext.writeAndFlush(response);
            //System.out.println(InnerAreaChecker.getHostIp(channelHandlerContext) + " was successfully handled");
        }

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (cause instanceof ReadTimeoutException) {
            ctx.close();
        } else {
            super.exceptionCaught(ctx, cause);
        }

    }

}

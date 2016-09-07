package utils;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;

import static io.netty.handler.codec.http.HttpHeaders.Names.*;

/**
 * Created by Baka on 02.09.2015.
 */
public class SendResponse {
    public static void flush(HttpRequest request, HttpResponseStatus httpResponseStatus, ChannelHandlerContext channelHandlerContext, String result) {
        String token = request.headers().get("Token");
        String uid = request.headers().get("UserId");

        boolean keepAlive = HttpHeaders.isKeepAlive(request);
        FullHttpResponse response = null;
        if (httpResponseStatus == null) {
            response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.copiedBuffer(result, CharsetUtil.UTF_8));
        } else {
            response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, httpResponseStatus, Unpooled.copiedBuffer(httpResponseStatus.toString(), CharsetUtil.UTF_8));
        }
        response.headers().set(CONTENT_TYPE, "text/plain");
        response.headers().set(CONTENT_LENGTH, response.content().readableBytes());

        if (token != null) {
            response.headers().set("Token", token);
            response.headers().set("UserId", uid);
        }

//        response.headers().add("Access-Control-Allow-Origin", "*");
//        response.headers().add("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS");
//        response.headers().add("Access-Control-Allow-Headers", "X-Requested-With, Content-Type, Content-Length");

        if (!keepAlive) {
            channelHandlerContext.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        } else {
            response.headers().set(CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
            channelHandlerContext.writeAndFlush(response);
            //System.out.println(InnerAreaChecker.getHostIp(channelHandlerContext) + " was successfully handled");
        }
    }

    public static void flushImg(HttpRequest request, ChannelHandlerContext channelHandlerContext, byte[] img) {
        String token = request.headers().get("Token");
        String uid = request.headers().get("UserId");

        boolean keepAlive = HttpHeaders.isKeepAlive(request);
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.copiedBuffer(img));

        response.headers().set(CONTENT_TYPE, "application/octet-stream");
        response.headers().set(CONTENT_LENGTH, response.content().readableBytes());

        if (token != null) {
            response.headers().set("Token", token);
            response.headers().set("UserId", uid);
        }

        if (!keepAlive) {
            channelHandlerContext.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        } else {
            response.headers().set(CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
            channelHandlerContext.writeAndFlush(response);
        }
    }
}

package utils;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.util.CharsetUtil;

/**
 * Created by Baka on 02.09.2015.
 */
public class PostBody {
    public static String getPostBody(HttpRequest httpRequest) {

        HttpContent httpContent = (HttpContent) httpRequest;
        ByteBuf content = httpContent.content();
        StringBuilder buf = new StringBuilder();
        buf.append(content.toString(CharsetUtil.UTF_8));
        return buf.toString();
    }
}

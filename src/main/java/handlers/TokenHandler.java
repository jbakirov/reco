package handlers;

import com.fasterxml.jackson.core.JsonParseException;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.timeout.ReadTimeoutException;
import io.netty.util.ReferenceCountUtil;
import models.FeedItem;
import models.User;
import redis.RedisManager;
import redis.clients.jedis.Jedis;
import utils.EmailValidator;
import utils.JsonHandler;
import utils.PostBody;
import utils.SendResponse;

import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.util.*;

/**
 * Created by Baka on 07.09.2015.
 */
@ChannelHandler.Sharable
public class TokenHandler extends SimpleChannelInboundHandler {

    HttpRequest request;


    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Object o) throws Exception {


        if (o instanceof HttpRequest) {
            this.request = (HttpRequest) o;
        }

        System.out.println(((InetSocketAddress) channelHandlerContext.channel().remoteAddress()).getAddress().getHostAddress());
        String token = "";
        String userId = "";
        try {
            token = request.headers().get("Token");
            userId = request.headers().get("UserId");
        } catch (Exception e) {
            e.printStackTrace();
        }


        if (token == null) {
            String input = PostBody.getPostBody(request);
            if (input == null || input.equals("")) {
                channelHandlerContext.close();
                return;
            }
            User user = (User) JsonHandler.toJava(input, "models.User");

            String t = generateToken();

            switch (user.getAction()){
                case "signup":
                    actionSignup(user, t, channelHandlerContext);
                    break;
                case "login":
                    actionLogin(user, channelHandlerContext, t);
                    break;
                case "logout":
                    actionLogout(user);
                    break;
                default:
                    channelHandlerContext.close();
                    break;
            }
        } else {
            // check token
            String resp = "";
            Jedis conn = null;
            try {
                conn = RedisManager.pool.getResource();
                resp = RedisManager.checkToken(conn, token, userId);
            } finally {
                if (conn != null){
                    conn.close();
                }
            }

            if (resp == null) {
                SendResponse.flush(request, HttpResponseStatus.FORBIDDEN, channelHandlerContext, "");
                channelHandlerContext.close();
                return;
            } else {
                if (userId == null){
                    SendResponse.flush(request, HttpResponseStatus.FORBIDDEN, channelHandlerContext, "");
                    return;
                }
                request.headers().set("Token", resp);
                request.headers().set("UserId", userId);
                ReferenceCountUtil.retain(o);
                channelHandlerContext.fireChannelRead(o);
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (cause instanceof ReadTimeoutException) {
            ctx.close();
        } else if(cause instanceof JsonParseException){
          ctx.close();
        } else{
            super.exceptionCaught(ctx, cause);
        }

    }

    public long registration(Jedis conn, User user, String token) {
        String email = user.getUser_email();

        if (user.getUser_name().length() < 4){
            return -5;
        }

        if (email.isEmpty() || email == null){
            return -10;
        }else {
            if (!isValidEmail(email)){
                return -15;
            }
        }

        long uid = RedisManager.createUser(conn, user.getUser_name(), user.getUser_email(), user.getPassword(), user.getProfilePic());

        if (uid > 0) {
            RedisManager.setToken(conn, token, String.valueOf(uid));
        }

        return uid;
    }

    public long login(Jedis conn, User user, String token, ChannelHandlerContext channelHandlerContext) {
        long resp = 0;

        if (RedisManager.ifUserExists(conn, user.getUser_name(), user.getPassword())) {
            String uid = conn.hget("user:", user.getUser_name());
            RedisManager.setToken(conn, token, uid);
            resp = 1;
        } else{
            SendResponse.flush(request, HttpResponseStatus.FORBIDDEN, channelHandlerContext,  "");
        }
        return resp;
    }

    public String generateToken() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }

    public boolean isValidEmail (String email){
        EmailValidator emailValidator = new EmailValidator();
        return emailValidator.validate(email);
    }

    public void actionLogin(User user, ChannelHandlerContext channelHandlerContext, String t){
        Jedis conn = null;
        long resp = 0;
        try {
            conn = RedisManager.pool.getResource();
            resp = login(conn, user, t, channelHandlerContext);
        } finally {
            if (conn != null){
                conn.close();
            }
        }

        if (resp > 0) {
            long uid = 0;
            List<FeedItem> feed = new ArrayList<>();
            try{
                conn = RedisManager.pool.getResource();
                uid = Long.parseLong(conn.hget("user:", user.getUser_name().toLowerCase()));
                feed.addAll(RedisManager.getHomePosts(conn, uid, 1, 7));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } finally {
                if (conn != null){
                    conn.close();
                }
            }
            request.headers().set("Token", t);
            request.headers().set("UserId", uid);
            SendResponse.flush(request, null, channelHandlerContext, JsonHandler.toJson(feed));
        }else {
            SendResponse.flush(request, HttpResponseStatus.FORBIDDEN, channelHandlerContext, "");
            channelHandlerContext.close();
        }
    }

    public void actionLogout(User user){
        Jedis conn = null;
        try {
            conn = RedisManager.pool.getResource();
            RedisManager.deleteToken(conn, null, user.getUser_name());
        } finally {
            if (conn != null){
                conn.close();
            }
        }
    }

    public void actionSignup (User user, String t, ChannelHandlerContext channelHandlerContext){
        Jedis conn = null;
        long uid = 0;
        try {
            conn = RedisManager.pool.getResource();
            uid = registration(conn, user, t);
        } finally {
            if (conn != null){
                conn.close();
            }
        }

        if (uid > 0) {
            request.headers().set("Token", t);
            request.headers().set("UserId", uid);
            SendResponse.flush(request, null, channelHandlerContext, String.valueOf(uid));
        } else {
            if (uid == -10){
                SendResponse.flush(request, null,channelHandlerContext, "no email");
                channelHandlerContext.close();
            }else if (uid == -15){
                SendResponse.flush(request, null, channelHandlerContext, "invalid email");
                channelHandlerContext.close();
            } else if (uid == -5){
                SendResponse.flush(request, null, channelHandlerContext, "short username");
                channelHandlerContext.close();
            } else if (uid == -2){
                SendResponse.flush(request, null, channelHandlerContext, "this username already exists");
                channelHandlerContext.close();
            } else {
                SendResponse.flush(request, HttpResponseStatus.BAD_REQUEST, channelHandlerContext, "");
                channelHandlerContext.close();
            }
        }
    }
}

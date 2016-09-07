package handlers;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.router.Routed;
import models.Comment;
import models.Item;
import redis.RedisManager;
import redis.clients.jedis.Jedis;
import utils.JsonHandler;
import utils.PostBody;
import utils.SendResponse;

/**
 * Created by Baka on 02.09.2015.
 */
@ChannelHandler.Sharable
public class CreatePost extends SimpleChannelInboundHandler<Routed> {


    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Routed routed) throws Exception {

        String input = PostBody.getPostBody(routed.request());
        Item item = (Item) JsonHandler.toJava(input, "models.Item");
        String result = "";

        Jedis conn = null;

        try{
            conn = RedisManager.pool.getResource();

            if (!isValidQuery(routed.request(), conn, item)){
                SendResponse.flush(routed.request(), HttpResponseStatus.BAD_REQUEST, channelHandlerContext, "");
            }

            switch (item.getType()) {
                case "movie":
                    result = createMoviesTvShowsRecommendation(conn, item);

                    break;
                case "tvshow":
                    result = createMoviesTvShowsRecommendation(conn, item);

                    break;
                case "music":
                    result = createMusicRecommendation(conn, item);
                    break;
                case "book":
                    result = createBookRecommendation(conn, item);

                    break;
                case "place":
                    result = createPlaceRecommendation(conn, item);

                    break;
                case "yvideo":
                    result = createYoutubeRecommendation(conn, item);
                    break;
            }
        }finally {
            if (conn != null){
                conn.close();
            }
        }

        SendResponse.flush(routed.request(), null, channelHandlerContext, result);

    }

    public String createMoviesTvShowsRecommendation(Jedis conn, Item item) {
        String result = "success";

            long pid = RedisManager.post(conn, Long.parseLong(item.getUid()), item.getName(),
                    String.valueOf(item.getId()), item.getType().toLowerCase(),
                    item.getGenres(), item.getInfo(), item.getPoster_url(), item.getFeedback());
            if (pid < 0) {
                result = "error";
            }

        return result;
    }

    public String createYoutubeRecommendation(Jedis conn, Item item) {
        String result = "success";

        long pid = RedisManager.post(conn, item);
        if (pid < 0) {
            result = "error";
        }

        return result;
    }

    public String createMusicRecommendation (Jedis conn, Item item){
        String result = "success";

        long pid = RedisManager.post(conn, Long.parseLong(item.getUid()), item.getName(),
                String.valueOf(item.getId()), item.getType().toLowerCase(),
                item.getGenres(), item.getInfo(), item.getPoster_url(), item.getFeedback());
        if (pid < 0) {
            result = "error";
        }

        return result;
    }

    public String createBookRecommendation(Jedis conn, Item item) {
        String result = "success";
            long pid = RedisManager.post(conn, Long.parseLong(item.getUid()), item.getName(), String.valueOf(item.getId()), item.getType(), item.getGenres(),
                    item.getInfo(), item.getPoster_url(), item.getCitation(), item.getFeedback(), null);
            if (pid < 0) {
                result = "error";
            }

        return result;
    }

    public String createPlaceRecommendation(Jedis conn, Item item) {
        String result = "success";
            long pid = RedisManager.post(conn, Long.parseLong(item.getUid()), item.getName(), String.valueOf(item.getId()), item.getType(), item.getCountry(), item.getCity(),
                    item.getInfo(), item.getPlacePhotos(), item.getLat(), item.getLon(), item.getFeedback());
            if (pid < 0) {
                result = "error";
            }

        return result;
    }

    public boolean isValidQuery(HttpRequest httpRequest, Jedis conn, Item item) {

        String token = httpRequest.headers().get("Token");
        String uid = String.valueOf(item.getUid());
        String uidFromRedis = conn.get("usertoken:" + uid);


        if (uidFromRedis != null) {
            if (uidFromRedis.equals(token)){
                return true;
            }
        }
        return false;
    }
}

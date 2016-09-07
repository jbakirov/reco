package handlers.search;

import dbmanager.Queries;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.router.Routed;
import models.Item;
import utils.JsonHandler;
import utils.SendResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Baka on 30.09.2015.
 */
@ChannelHandler.Sharable
public class SearchItemDb extends SimpleChannelInboundHandler<Routed>{

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Routed routed) throws Exception {
        String params = routed.request().getUri();
        QueryStringDecoder queryStringDecoder = new QueryStringDecoder(params);
        String uid = queryStringDecoder.parameters().get("uid").get(0);
        String type = queryStringDecoder.parameters().get("type").get(0);
        String value = queryStringDecoder.parameters().get("value").get(0);
        String lang = queryStringDecoder.parameters().get("lang").get(0);
        String option = queryStringDecoder.parameters().get("opt").get(0);

        List<Item> result = new ArrayList<>();

        if (option.equals("genre")){
            switch (type){
                case "book":
                    result = Queries.getBooksByGenre(value, lang);
                    break;
                case "movie":
                    result = Queries.getMoviesByGenre(value, lang);
                    break;
                case "tvshow":
                    result = Queries.getTvShowsByGenre(value, lang);
                    break;
                case "music":
                    result = Queries.getMusicByGenre(value);
                    break;
                case "place":
                    break;
                case "yvideo":
                    break;
            }
        }else {
            switch (type){
                case "book":
                    result = Queries.getBooks(value, lang);
                    break;
                case "movie":
                    result = Queries.getMovies(value, lang);
                    break;
                case "tvshow":
                    result = Queries.getTvShows(value, lang);
                    break;
                case "music":
                    result = Queries.getMusic(value);
                    break;
                case "place":
                    break;
                case "yvideo":
                    break;
            }
        }



        SendResponse.flush(routed.request(), null, channelHandlerContext, JsonHandler.toJson(result));

    }

}

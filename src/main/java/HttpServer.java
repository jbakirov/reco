import handlers.*;
import handlers.add.AddNewItem;
import handlers.picmanager.DownloadPicture;
import handlers.search.SearchItemDb;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.http.router.Handler;
import io.netty.handler.codec.http.router.Router;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.util.SelfSignedCertificate;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.io.InputStream;
import java.security.cert.CertificateException;
import java.util.Properties;

/**
 * Created by Baka on 16.07.2015.
 */
public class HttpServer {
    static final boolean SSL = System.getProperty("ssl") != null;
    static final int PORT = Integer.parseInt(System.getProperty("port", SSL ? "8443" : setPort()));


    public static void main(String[] args) {
        HttpServer httpServer = new HttpServer();
        httpServer.start();
    }

    public void start() {
        final SslContext sslContext;

        if (SSL) {
            try {
                //SslProvider provider = OpenSsl.isAlpnSupported()? SslProvider.OPENSSL : SslProvider.JDK;
                SelfSignedCertificate ssc = new SelfSignedCertificate();
                sslContext = SslContext.newServerContext(ssc.certificate(), ssc.privateKey());

            } catch (CertificateException e) {
                e.printStackTrace();
            } catch (SSLException e) {
                e.printStackTrace();
            }
        }

        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childOption(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000) //sets the timeout for the channel
//                    .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                    .childHandler(new ServerInitializer());
            //            LuceneManager luceneManager = new LuceneManager();
//            luceneManager.createIndexForBooks();
            ChannelFuture channelFuture = b.bind(PORT).sync();
            System.out.println("[Listening on port " + setPort() + "]");
            channelFuture.channel().closeFuture().sync();

        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }


    private class ServerInitializer extends ChannelInitializer<SocketChannel> {
        Router router = new Router()
                .GET("/handlers/comments", new GetAllComments())
                .GET("/handlers/usersearch", new UserSearch())
                .GET("/handlers/search", new SearchItem())
                .GET("/handlers/likes", new GetAllLikes())
                .POST("/handlers/follow", new FollowUser())
                .POST("/handlers/ask", new AskQuestion())
                .POST("/handlers/post", new CreatePost())
                .POST("/handlers/mainfeed", new MainFeed())
                .POST("/handlers/comment", new CommentPost())
                .POST("/handlers/like", new LikePost())
                .POST("/handlers/unfollow", new UnfollowUser())
                .POST("/handlers/profilefeed", new ProfileFeed())
                .POST("/handlers/userpage", new GetUserPage())
                .POST("/handlers/unlike", new UnlikePost())
                .POST("/handlers/deletecomment", new DeleteComment())
                .GET("/handlers/deletepost", new DeletePost())
                .GET("/handlers/allfollowers", new GetAllFollowers())
                .GET("/handlers/allfollowings", new GetAllFollowings())
                .POST("/handlers/fullview", new ItemView())
                        //item search
                .GET("/handlers/search/getitem", new SearchItemDb())

                        // add new item
                .POST("/handlers/add/newitem", new AddNewItem())

                .GET("/handlers/picmanager/downloadpic", new DownloadPicture())

                .GET("/handlers/userq", new GetUserQuestions())
                .GET("/handlers/questionsbytag", new GetQuestionsByTag())

                .GET("/handlers/fullquestion", new GetFullQuestionWithAnswers())
                .GET("/handlers/getanswers", new GetAnswers())
                .GET("/hanlders/fquestions", new GetFriendsQuestions())

                .POST("handlers/answer", new AnswerToQuestion())

                .GET("/handlers/getten", new GetTop10())
                .GET("/handlers/addfavorite", new AddToFavorites())
                .GET("/handlers/getfavorites", new GetUserFavoritePosts())
                .POST("/handlers/upload", new Upload())
                .GET("/handlers/getfavitems", new GetUserFavoriteItems());


        Handler handler = new Handler(router);

        @Override
        public void initChannel(SocketChannel ch) throws Exception {
            ChannelPipeline p = ch.pipeline();
//            p.addLast("loggerr", new LoggingHandler());
            p.addLast("decoder", new HttpRequestDecoder());
            p.addLast("aggregator", new HttpObjectAggregator(1048576));
            p.addLast("encoder", new HttpResponseEncoder());
            p.addLast("t", new TokenHandler());
            p.addLast(handler.name(), handler);
//            p.addLast(new ReadTimeoutHandler(5));
        }
    }


    public static String setPort() {
        String port = "";

        try {
            Properties properties = new Properties();
            InputStream is = HttpServer.class.getResourceAsStream("config.properties");
            properties.load(is);
            port = properties.getProperty("server.port");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return port;
    }
}

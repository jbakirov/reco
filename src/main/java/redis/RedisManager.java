package redis;

import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;
import models.*;
import redis.clients.jedis.*;
import utils.SecurePassword;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Created by Baka on 23.08.2015.
 */
public class RedisManager {

    // sname - second name
    // name - first name
    // login - nickname
    // ranswers - right answers
    // rep - reputation
    // recos - recommendations

    // book
    // iname - itemName
    // desc - description
    // purl - poster URL
    // cit - citation
    // fback - feedback
    //post:scr - screenshots

    // user:questions: + uid - stores all questions

    private static int HOME_TIMELINE_SIZE = 1000;
    private static int POSTS_PER_PASS = 1000;
    private static int REFILL_USERS_STEP = 50;

    public static JedisPool pool = new JedisPool(new JedisPoolConfig(), "localhost", 6379, 10 * 1000);

    /**
     * favorite recommendations
     */
    public static boolean addToFavorites(Jedis conn, long itemId, long uid, String type) {
        long now = System.currentTimeMillis();
        Transaction trans = conn.multi();
        trans.zadd("user:fav:" + type + ":" + uid, now, String.valueOf(itemId));
        trans.exec();
        return true;
    }

    public static List<FeedItem> getUserFavPosts (Jedis conn, long uid, int page, int count){

        Set<String> postIds = conn.zrevrange("user:fav:" + uid, (page - 1) * count, page * count - 1);

        Transaction trans = conn.multi();
        for (String id : postIds) {
            trans.hgetAll("post:" + id);
        }

        List<FeedItem> feedItems = new ArrayList<>();

        for (Object result : trans.exec()) {
            Map<String, String> post = (Map<String, String>) result;

            if (post != null && post.size() > 0) {
                FeedItem feedItem = null;
                String type = post.get("type");
                switch (type) {
                    case "book":
                        feedItem = (FeedItem) getBookPosts(conn, uid, 0, post);
                        break;
                    case "movie":
                        feedItem = (FeedItem) getMovieTvShowPosts(conn, uid, 0, post);
                        break;
                    case "tvshow":
                        feedItem = (FeedItem) getMovieTvShowPosts(conn, uid, 0, post);
                        break;
                    case "yvideo":
                        feedItem = (FeedItem) getYvideoPosts(conn, uid, 0, post);
                        break;
                    case "place":
                        break;
                    case "music":
                        break;
                }

                feedItems.add(feedItem);
            }
        }

        return feedItems;
    }

    public static Set<String> getUserFavItems(Jedis conn, long uid, int page, int count, String type){
        return conn.zrevrange("user:fav:" + type + ":" + uid, (page - 1) * count, page * count - 1);

    }

    public static Item getFullItemView(Jedis conn, String itemId, String type) {

        Item item = new Item();
        Double recosM = 0d;
        switch (type) {
            case "book":
                recosM = conn.zscore("book:recos", itemId);
                Set<String> citations = conn.zrange("b:cit:" + itemId, 0, 4);

                recosM = recosM == null ? 0 : recosM;
                item.setRecos_count(recosM.toString());
                item.setCitation(getRandomItemFromList(citations));

                break;
            case "movie":

                Set<String> screenShots = conn.zrange("m:scr:" + itemId, 0, -1);
                Set<String> citationsM = conn.zrange("m:cit:" + itemId, 0, -1);
                recosM = conn.zscore("movie:recos", itemId);
                Set<String> trailerUrl = conn.zrange("m:trailerurl:" + itemId, 0, -1);

                recosM = recosM == null ? 0 : recosM;
                item.setRecos_count(recosM.toString());
                item.setCitation(getRandomItemFromList(citationsM));
                item.setScreenShots(getCommaSeparatedScreenShots(screenShots));
                item.setTrailerUrl(getRandomItemFromList(trailerUrl));

                break;
            case "yvideo":

                recosM = conn.zscore("yvideo:recos", itemId);

                recosM = recosM == null ? 0 : recosM;
                item.setRecos_count(recosM.toString());
                break;
            case "tvshow":
                break;
            case "music":
                break;
        }
        return item;

    }

    private static String getCommaSeparatedScreenShots(Set<String> set) {
        String res = "";
        for (String s : set) {
            res = res + s + ", ";
        }
        return res;
    }

    private static String getRandomItemFromList(Set<String> items) {
        int size = items.size();
        int randomIndex = (int) (Math.random() * size - 1);
        int i = 0;
        String res = "";
        for (String s : items) {
            if (i == randomIndex) {
                res = s;
                break;
            }
            i++;
        }
        return res;
    }

    public static void setProfilePic(Jedis conn, String url, String uid) {
        Pipeline p = conn.pipelined();
        p.hset("user:" + uid, "profilePic", url);
        p.sync();
    }

    public static List<User> getAllFollowings(Jedis conn, String uid, String ruid) {
        Pipeline p = conn.pipelined();
        Response<Set<String>> following = p.zrange("following:" + uid, 0, -1);
        Response<Set<String>> followingR = p.zrange("following:" + ruid, 0, -1);
        p.sync();

        if (following.get() == null) {
            return Collections.emptyList();
        }

        Boolean isFollowing = false;
        List<User> users = new ArrayList<>();
        for (String s : following.get()) {
            User user = new User();

            p = conn.pipelined();
            Response<String> username = p.hget("user:" + s, "login");
            Response<String> profilePic = p.hget("user:" + s, "profilePic");
            p.sync();

            if (s.equals(ruid)) {
                isFollowing = null;
            } else if (followingR.get().contains(s)) {
                isFollowing = true;
            }

            user.setUser_name(username.get());
            user.setProfilePic(profilePic.get());
            user.setIsFollowing(isFollowing);
            user.setId(Long.parseLong(s));
            users.add(user);
            isFollowing = false;
        }
        return users;
    }

    public static List<User> getAllFollowers(Jedis conn, String uid, String ruid) {
        Pipeline p = conn.pipelined();
        Response<Set<String>> followers = p.zrange("followers:" + uid, 0, -1);
        Response<Set<String>> following = p.zrange("following:" + ruid, 0, -1);
        p.sync();
        if (followers.get() == null) {
            return Collections.emptyList();
        }

        List<User> users = new ArrayList<>();

        Boolean isFollowing = false;
        for (String s : followers.get()) {
            User user = new User();

            p = conn.pipelined();
            Response<String> username = p.hget("user:" + s, "login");
            Response<String> profilePic = p.hget("user:" + s, "profilePic");
            p.sync();

            if (s.equals(ruid)) {
                isFollowing = null;
            } else if (following.get().contains(s)) {
                isFollowing = true;
            }

            user.setUser_name(username.get());
            user.setProfilePic(profilePic.get());
            user.setId(Long.parseLong(s));
            user.setIsFollowing(isFollowing);
            users.add(user);
            isFollowing = false;
        }
        return users;
    }

    public static List<User> getAllLikes(Jedis conn, String pid, String ruid) {

        Pipeline p = conn.pipelined();
        Response<Set<String>> likes = p.zrange("postl:id:" + pid, 0, -1);
        Response<Set<String>> followingUsers = p.zrange("following:" + ruid, 0, -1);
        p.sync();

        List<User> users = new ArrayList<>();
        for (String s : likes.get()) {
            Boolean isFollowing = false;
            if (ruid.equals(s)) {
                isFollowing = null;
            }
            User user = new User();

            p = conn.pipelined();
            Response<String> username = p.hget("user:" + s, "login");
            Response<String> profile_pic = p.hget("user:" + s, "profilePic");
            p.sync();

            user.setUser_name(username.get());
            user.setProfilePic(profile_pic.get());
            user.setId(Long.parseLong(s));
            if (followingUsers.get().contains(s)) {
                isFollowing = true;
            }
            user.setIsFollowing(isFollowing);
            users.add(user);
        }

        return users;
    }

    public static List<User> getUserSearch(Jedis conn, String prefix, int count, long suid) {
        List<String> usernames = autoComplete(prefix, count, conn);
        List<User> users = new ArrayList<>();
        Set<String> followingUsers = conn.zrange("following:" + suid, 0, -1);
        for (String username : usernames) {
            User user = new User();

            Pipeline p = conn.pipelined();
            Response<String> uid = p.hget("user:", username);
            Response<String> profile_pic = p.hget("user:" + uid, "profilePic");
            p.sync();

            Boolean isFollowing = false;
            if (uid.equals(String.valueOf(suid))) {
                isFollowing = null;
            } else if (followingUsers.contains(String.valueOf(uid.get()))) {
                isFollowing = true;
            }

            user.setId(Long.parseLong(uid.get()));
            user.setUser_name(username);
            user.setProfilePic(profile_pic.get());
            user.setIsFollowing(isFollowing);
            users.add(user);
        }
        return users;
    }

    public static boolean editComment(Jedis conn, long uid, long cid, String newComment) {
        Long userId = Long.parseLong(conn.hget("comment:" + cid, "uid"));
        if (userId != uid) {
            return false;
        }

        Pipeline pipeline = conn.pipelined();
        pipeline.hset("comment:" + cid, "comment", newComment);
        pipeline.sync();
        return true;
    }

    public static boolean editAnswer(Jedis conn, long uid, long aid, String newAnswer) {

        Pipeline pipe = conn.pipelined();
        Response<String> isCorrect = pipe.hget("answer:" + aid, "isCorrect");
        Response<String> userId = pipe.hget("answer:" + aid, "uid");
        pipe.sync();

        if (Long.parseLong(userId.get()) != uid || isCorrect.get().equals("true")) {
            return false;
        }

        pipe = conn.pipelined();
        pipe.hset("answer:" + aid, "answer", newAnswer);
        pipe.sync();
        return true;

    }

    public static boolean editQuestion(Jedis conn, long uid, long qid, String newQuestion) {
        if (!conn.hget("question:" + qid, "uid").equals(String.valueOf(uid))) {
            return false;
        }

        Pipeline pipeline = conn.pipelined();
        pipeline.hset("question:" + qid, "question", newQuestion);
        pipeline.sync();
        return true;
    }

    public static boolean editPost(Jedis conn, long uid, long pid, String newOpinion) {

        if (!conn.hget("post:" + pid, "uid").equals(String.valueOf(uid))) {
            return false;
        }

        Pipeline pipeline = conn.pipelined();
        pipeline.hset("post:" + pid, "opinion", newOpinion);
        pipeline.sync();
        return true;
    }

    public static boolean reportOnPost(Jedis conn, long pid, long uid, long ruid, String message) {
        if (conn.hget("report:" + pid, "id") != null) {
            return false;
        }

        Transaction trans = conn.multi();
        trans.incr("report:id:");
        List<Object> response = trans.exec();
        long rid = (Long) response.get(0);

        trans = conn.multi();
        Map<String, String> data = new HashMap<>();
        data.put("id", String.valueOf(rid));
        data.put("uid", String.valueOf(uid));
        data.put("ruid", String.valueOf(ruid));
        data.put("message", message);
        data.put("time", String.valueOf(System.currentTimeMillis()));

        trans.hmset("report:" + pid, data);
        trans.expire("report:" + pid, 60 * 30);
        trans.exec();

        // do not forget to notify by email!

        return true;
    }

    public static boolean ifUserExists(Jedis conn, String username, String password) {

        String uid = conn.hget("user:", username.toLowerCase());
        Transaction trans = conn.multi();
        trans.hget("user:" + uid, "login");
        trans.hget("user:" + uid, "hpass");
        trans.hget("user:" + uid, "salt");

        List<Object> response = trans.exec();
        String login = (String) response.get(0);
        String hpass = (String) response.get(1);
        String salt = (String) response.get(2);

        if (login != null) {
            if (SecurePassword.isExpectedPassword(password.toCharArray(), Base64.decode(salt), Base64.decode(hpass))) {
                return true;
            }
        }

        return false;
    }

    public static long createPost(Jedis conn, long uid, String itemName, String type, String item_id, String genres, String author, String posterUrl, String citation, String feedback, Map<String, String> data) {

        Transaction trans = conn.multi();
        trans.hget("user:" + uid, "login");
        trans.incr("post:id:");

        List<Object> response = trans.exec();
        String login = (String) response.get(0);
        long id = (Long) response.get(1);

        if (login == null) {
            return -1;
        }
        if (data == null) {
            data = new HashMap<String, String>();
        }

        data.put("id", String.valueOf(id));
        data.put("uid", String.valueOf(uid));
        data.put("username", login);
        data.put("iname", itemName);
        data.put("type", type);
        data.put("genre", genres);
        data.put("author", author);
        data.put("comments", "0");
        data.put("likes", "0");
        data.put("purl", posterUrl);
        data.put("feedback", feedback);
        data.put("itemid", item_id);
        data.put("date", String.valueOf(System.currentTimeMillis()));

        trans = conn.multi();

        trans.hincrBy("book:recos:" + item_id, "recos", 1);
        trans.hmset("post:" + id, data);
        trans.hincrBy("user:" + uid, "recos", 1);
        trans.exec();
        return id;
    }

    public static long createPost(Jedis conn, long uid, String itemName, String item_id,
                                  String type, String genres, String description, String posterUrl, String feedback, Map<String, String> data) {

        Transaction trans = conn.multi();
        trans.hget("user:" + uid, "login");
        trans.incr("post:id:");

        List<Object> response = trans.exec();
        String login = (String) response.get(0);
        long id = (Long) response.get(1);

        if (login == null) {
            return -1;
        }
        if (data == null) {
            data = new HashMap<String, String>();
        }

        data.put("id", String.valueOf(id));
        data.put("uid", String.valueOf(uid));
        data.put("username", login);
        data.put("iname", itemName);
        data.put("type", type);
        data.put("genre", genres);
        data.put("info", description);
        data.put("comments", "0");
        data.put("likes", "0");
        data.put("purl", posterUrl);
        data.put("feedback", feedback);
        data.put("date", String.valueOf(System.currentTimeMillis()));
        data.put("itemid", item_id);


        switch (type) {
            case "movie":
//                trans.hincrBy("movie:recos:" + item_id, "recos", 1);
                recoCounter("movie", conn, item_id);
                break;
            case "tvshow":
                recoCounter("tvshow", conn, item_id);
//                trans.hincrBy("tvshow:recos:" + item_id, "recos", 1);
                break;
//            case "yvideo":
//                recoCounter("yvideo", conn, item_id);
//                data.put("channel", item.getChannel());
//                data.put("chid", String.valueOf(item.getChannelId()));
//                data.put("genreid", String.valueOf(item.getGenreId()));
////                trans.hincrBy("yvideo:recos:" + item_id, "recos", 1);
//                break;
            case "music":
                recoCounter("music", conn, item_id);
//                trans.hincrBy("music:recos:" + item_id, "recos", 1);
        }

        trans = conn.multi();
        trans.hmset("post:" + id, data);
        trans.hincrBy("user:" + uid, "recos", 1);
        trans.exec();

        return id;
    }

    public static long createPost(Jedis conn, Item item, Map<String, String> data){
        Transaction trans = conn.multi();
        trans.hget("user:" + item.getUid(), "login");
        trans.incr("post:id:");

        List<Object> response = trans.exec();
        String login = (String) response.get(0);
        long id = (Long) response.get(1);

        if (login == null) {
            return -1;
        }
        if (data == null) {
            data = new HashMap<String, String>();
        }

        data.put("id", String.valueOf(id));
        data.put("uid", String.valueOf(item.getUid()));
        data.put("username", login);
        data.put("iname", item.getName());
        data.put("type", item.getType());
        data.put("genre", item.getGenres());
        data.put("comments", "0");
        data.put("likes", "0");
        data.put("purl", item.getPoster_url());
        data.put("feedback", item.getFeedback());
        data.put("date", String.valueOf(System.currentTimeMillis()));
        data.put("itemid", String.valueOf(item.getId()));
        data.put("channel", item.getChannel());
        data.put("chid", String.valueOf(item.getChannelId()));
        data.put("genreid", String.valueOf(item.getGenreId()));

        recoCounter("yvideo", conn, String.valueOf(item.getId()));

        trans = conn.multi();
        trans.hmset("post:" + id, data);
        trans.hincrBy("user:" + item.getUid(), "recos", 1);
        trans.exec();

        return id;
    }

    private static void recoCounter(String type, Jedis conn, String item_id){
        Transaction trans = conn.multi();
        trans.zincrby(type + ":recos", 1, item_id);
        trans.exec();
    }

    public static Set<String> getTop10 (String type, Jedis conn){
        Set<String> items = conn.zrevrange( type + ":recos", 0, 9);

        return items;
    }

    public static void createNewItem(Jedis conn, String itemId, String screenShots, String citation, String trailerurl, String type) {

        Transaction trans = conn.multi();
        switch (type) {
            case "book":
                if (citation != null) {
                    trans.zadd("b:cit:" + itemId, 0, citation);
                }
                trans.zremrangeByRank("b:cit:" + itemId, 0, -10);
                break;
            case "movie":
                if (trailerurl != null) {
                    trans.zadd("m:trailerurl:" + itemId, 0, trailerurl);
                }
                if (screenShots != null) {
                    trans.zadd("m:scr:" + itemId, 0, screenShots);
                }
                if (citation != null) {
                    trans.zadd("m:cit:" + itemId, 0, citation);
                }
                trans.zremrangeByRank("m:scr:" + itemId, 0, -6);
                trans.zremrangeByRank("m:cit:" + itemId, 0, -6);
                trans.zremrangeByRank("m:trailerurl:" + itemId, 0, 1);
                break;
            case "tvshow":
                if (trailerurl != null) {
                    trans.zadd("tvs:trailerurl:" + itemId, 0, trailerurl);
                }
                if (screenShots != null) {
                    trans.zadd("tvs:scr:" + itemId, 0, screenShots);
                }
                if (citation != null) {
                    trans.zadd("tvs:cit:" + itemId, 0, citation);
                }
                trans.zremrangeByRank("tvs:scr:" + itemId, 0, -6);
                trans.zremrangeByRank("tvs:cit:" + itemId, 0, -6);
                trans.zremrangeByRank("tvs:trailerurl:" + itemId, 0, 1);
                break;
        }

        trans.exec();
    }

//    private static void createNewItem(Jedis conn, int uid, long yvidId, long channelId, String channelName, long genreId, String genreName){
//        Transaction trans = conn.multi();
//        trans.zadd("yv:chname:" + yvidId, 0, channelName);
//        trans.zadd("yv:chid:" + yvidId, 0, String.valueOf(channelId));
//        trans.zadd("yv:gid" + yvidId,)
//
//
//    }


    public static long createPost(Jedis conn, long uid, String itemName, String item_id,
                                  String type, String country, String city, String posterUrl,
                                  String[] placePhotos, String lat, String lon, String feedback, Map<String, String> data) {

        Transaction trans = conn.multi();
        trans.hget("user:" + uid, "login");
        trans.incr("post:id:");

        List<Object> response = trans.exec();
        String login = (String) response.get(0);
        long id = (Long) response.get(1);

        if (login == null) {
            return -1;
        }
        if (data == null) {
            return -1;
        }

        data.put("id", String.valueOf(id));
        data.put("uid", String.valueOf(uid));
        data.put("username", login);
        data.put("iname", itemName);
        data.put("type", type);
        data.put("country", country);
        data.put("city", city);
        data.put("purl", posterUrl);
        data.put("comments", "0");
        data.put("likes", "0");
        data.put("feedback", feedback);
        data.put("itemid", item_id);
        data.put("date", String.valueOf(System.currentTimeMillis()));

        String urls = "";
        if (placePhotos != null) {
            for (String s : placePhotos) {
                urls = urls + " " + s;
            }
        }

        trans = conn.multi();
        trans.zadd("place:scr:" + item_id, 0, urls); // adding photos into place object
        trans.zremrangeByRank("place:scr:" + item_id, 0, -6);
//        trans.sadd("post:scr:" + id, urls); // adding photos into current post
        trans.hincrBy("place:recos:" + item_id, "recos", 1); // counting place recommendations
//        trans.sadd(city, item_id);              // adding places ids into city key
        trans.hmset("post:" + id, data);
        trans.hincrBy("user:" + uid, "recos", 1);
        trans.exec();

        return id;
    }

    public static long post(Jedis conn, long uid, String itemName, String item_id, String type,
                            String genres, String author, String posterUrl,
                            String citation, String feedback, Map<String, String> data) {
        long id = createPost(conn, uid, itemName, type, item_id, genres, author, posterUrl, citation, feedback, data);
        if (id == -1) {
            return -1;
        }

        String postedString = conn.hget("post:" + id, "date"); // date
        if (postedString == null) {
            return -1;
        }

        long posted = Long.parseLong(postedString);
        conn.zadd("profile:" + uid, posted, String.valueOf(id));

        syndicateStatus(conn, uid, id, posted, 0);
        return id;
    }

    public static long post(Jedis conn, long uid, String itemName, String item_id, String type,
                            String genre, String info, String posterUrl, String feedback) {
        long id = createPost(conn, uid, itemName, item_id, type, genre, info, posterUrl, feedback, null);
        if (id == -1) {
            return -1;
        }

        String postedString = conn.hget("post:" + id, "date"); // date
        if (postedString == null) {
            return -1;
        }

        long posted = Long.parseLong(postedString);
        conn.zadd("profile:" + uid, posted, String.valueOf(id));

        syndicateStatus(conn, uid, id, posted, 0);
        return id;
    }

    public static long post(Jedis conn, Item item){
        long id = createPost(conn, item, null);
        if (id == -1) {
            return -1;
        }

        String postedString = conn.hget("post:" + id, "date"); // date
        if (postedString == null) {
            return -1;
        }

        long posted = Long.parseLong(postedString);
        conn.zadd("profile:" + item.getUid(), posted, String.valueOf(id));

        syndicateStatus(conn, Long.valueOf(item.getUid()), id, posted, 0);
        return id;
    }

    public static long post(Jedis conn, long uid, String itemName, String item_id, String type, String country, String city, String description, String[] placePhotos, String lat, String lon, String feedback) {
        long id = createPost(conn, uid, itemName, item_id, type, country, city, description, placePhotos, lat, lon, feedback, null);
        if (id == -1) {
            return -1;
        }

        String postedString = conn.hget("post:" + id, "date"); // date
        if (postedString == null) {
            return -1;
        }

        long posted = Long.parseLong(postedString);
        conn.zadd("profile:" + uid, posted, String.valueOf(id));

        syndicateStatus(conn, uid, id, posted, 0);
        return id;
    }

    public static boolean deleteToken(Jedis conn, String token, String userId) {

        String t = "";
        if (token == null) {
            t = conn.get("usertoken:" + userId);
        }

        Pipeline p = conn.pipelined();
        p.del("usertoken:" + userId);
        p.srem("tokens", t);
        p.sync();

        return true;
    }

    public static String checkToken(Jedis conn, String token, String uid) {
//        String lusername = username.toLowerCase();
        Pipeline p = conn.pipelined();
        Response<Boolean> isMember = p.sismember("tokens", token);
        Response<String> getToken = p.get("usertoken:" + uid);
        p.sync();


        if (getToken.get() == null) {
            String newToken = "";
            if (isMember.get()) {
                p = conn.pipelined();
                p.del("usertoken:" + uid);
                p.srem("tokens", token);
                p.sync();

                newToken = setToken(conn, generateToken(), uid);
            } else {
                return null;
            }
            return newToken;
        } else {
            if (!getToken.get().equals(token)) {
                return null;
            }
        }
        return token;
    }

    public static String generateToken() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }

    public static void updateToken(Jedis conn, String token, String username) {
        long timestamp = System.currentTimeMillis();
        Pipeline pipeline = conn.pipelined();
        pipeline.hset("login:" + token, "uname", username);
//        pipeline.expire("login:" + token, 3600);
//        pipeline.zadd("recent:", timestamp, username);
        pipeline.sync();
    }

    public static String setToken(Jedis conn, String token, String uid) {

        String newToken = token;
        Pipeline p = null;
//        String lusername = username.toLowerCase();

        String k = conn.get("usertoken:" + uid);

        if (k != null) {
            if (!k.equals(token)) {
                p = conn.pipelined();
                p.del("usertoken:" + uid);
                p.srem("tokens", k);
                p.sync();
            }
        }

        p = conn.pipelined();
        p.set("usertoken:" + uid, newToken);
        p.expire("usertoken:" + uid, 10 * 60);
        p.sadd("tokens", token);
        p.sync();

        return newToken;
    }

    @SuppressWarnings("unchecked")
    public static List<Map<String, String>> getAnswers(Jedis conn, long qid, int page, int count) {
        Set<String> answerIds = conn.zrevrange("questiona:id:" + qid, (page - 1) * count, page * count - 1);

        Transaction trans = conn.multi();
        for (String id : answerIds) {
            trans.hgetAll("answer:" + id);
        }

        List<Map<String, String>> answers = new ArrayList<>();
        for (Object result : trans.exec()) {
            Map<String, String> answer = (Map<String, String>) result;
            if (answer != null && answer.size() > 0) {
                answers.add(answer);
            }
        }
        return answers;
    }

    public static boolean markAsCorrect(Jedis conn, long uid, long aid, long qid) {

        if (!String.valueOf(uid).equals(conn.hget("answer:" + aid, "uid"))) {
            return false;
        }

        Transaction trans = conn.multi();
        trans.hset("answer:" + aid, "isCorrect", "true");
        trans.hincrBy("user:" + uid, "ranswers", 1);
        trans.hincrBy("user:" + uid, "rep", 3);
        trans.zadd("user:ranswers:" + uid, System.currentTimeMillis(), String.valueOf(qid));
        trans.exec();

        return true;
    }

    public static boolean unmarkAnswer(Jedis conn, long uid, long aid, long qid) {
        if (!conn.hget("answer:" + aid, "isCorrect").equals("true")) {
            return false;
        }

        Pipeline pipeline = conn.pipelined();
        pipeline.hset("answer:" + aid, "isCorrect", "false");
        pipeline.hincrBy("user:" + uid, "ranswers", -1);
        pipeline.hincrBy("user:" + uid, "rep", -3);
        pipeline.zrem("user:ranswers:" + uid, String.valueOf(qid));
        pipeline.sync();

        return true;
    }

    @SuppressWarnings("unchecked")
    public static List<Map<String, String>> getUserRightAnswers(Jedis conn, long uid, int page, int count) {
        Set<String> answerIds = conn.zrevrange("user:ranswers:" + uid, (page - 1) * count, page * count - 1);

        Transaction trans = conn.multi();
        for (String id : answerIds) {
            trans.hgetAll("answer:" + id);
        }

        List<Map<String, String>> answers = new ArrayList<Map<String, String>>();
        for (Object result : trans.exec()) {
            Map<String, String> answer = (Map<String, String>) result;
            if (answer != null && answer.size() > 0) {
                answers.add(answer);
            }
        }
        return answers;
    }

    public static long askQuestion(Jedis conn, long uid, String username, String question, String tag) {
        Transaction trans = conn.multi();
        trans.incr("question:id:");

        List<Object> response = trans.exec();
        long id = (Long) response.get(0);
        long now = System.currentTimeMillis();

        Map<String, String> data = new HashMap<>();
        data.put("id", String.valueOf(id));
        data.put("uid", String.valueOf(uid));
        data.put("question", question);
        data.put("uname", username);
        data.put("date", String.valueOf(now));
        data.put("views", "0");

        String[] array = tag.split(",");

        trans = conn.multi();
        trans.hmset("question:" + id, data);
        trans.hincrBy("user:" + uid, "questions", 1);
        trans.zadd("allquestions", now, String.valueOf(id));
        trans.zadd("user:questions:" + uid, now, String.valueOf(id));
        if (array.length <= 3 && array.length > 0) {
            for (int i = 0; i < array.length; i++) {
                trans.zadd(array[i].trim(), 0, String.valueOf(id));
            }
        }
        trans.exec();

        return id;
    }

    public static boolean deleteQuestion(Jedis conn, long id, long uid) {
        String key = "question:lock:" + id;
        String lock = acquireLockWithTimeout(conn, key, 1, 10);
        if (lock == null) {
            return false;
        }

        try {
            if (!String.valueOf(uid).equals(conn.hget(key, "uid"))) {
                return false;
            }

            Transaction trans = conn.multi();
            trans.zrem("user:questions:" + uid, String.valueOf(id));
            trans.hincrBy("user:" + uid, "questions", -1);
            trans.exec();

            return true;
        } finally {
            releaseLock(conn, key, lock);
        }
    }

    @SuppressWarnings("unchecked")
    public static List<Map<String, String>> getAllQuestions(Jedis conn, int page, int count) {
        Set<String> questionId = conn.zrevrange("allquestions", (page - 1) * count, page * count - 1);

        Transaction trans = conn.multi();
        for (String id : questionId) {
            trans.hgetAll(id);
        }

        List<Map<String, String>> questions = new ArrayList<>();
        for (Object result : trans.exec()) {
            Map<String, String> question = (Map<String, String>) result;
            if (question != null && question.size() > 0) {
                questions.add(question);
            }
        }

        return questions;
    }

    public static List<Map<String, String>> getAllQuestionsByTag(Jedis conn, int page, int count, String tag) {
        Set<String> questionId = conn.zrevrange(tag, (page - 1) * count, page * count - 1);

        Transaction trans = conn.multi();
        for (String id : questionId) {
            trans.hgetAll("question:" + id);
        }

        List<Map<String, String>> questions = new ArrayList<>();
        for (Object result : trans.exec()) {
            Map<String, String> question = (Map<String, String>) result;
            if (question != null && question.size() > 0) {
                questions.add(question);
            }
        }
        return questions;
    }

    public static List<Map<String, String>> getFriendsQuestions(Jedis conn, long fid, int page, int count) {
        Set<String> questionIds = conn.zrevrange("user:questions:" + fid, (page - 1) * count, page * count - 1);
        Transaction trans = conn.multi();
        for (String id : questionIds) {
            trans.hgetAll("question:" + id);
        }

        List<Map<String, String>> questions = new ArrayList<>();
        for (Object result : trans.exec()) {
            Map<String, String> question = (Map<String, String>) result;
            if (question != null && question.size() > 0) {
                questions.add(question);
            }
        }
        return questions;
    }

    @SuppressWarnings("unchecked")
    public static List<Map<String, String>> getUserQuestions(Jedis conn, long uid, int page, int count) {
        Set<String> questionIds = conn.zrevrange("user:questions:" + uid, (page - 1) * count, page * count - 1);

        Transaction trans = conn.multi();
        for (String id : questionIds) {
            trans.hgetAll("question:" + id);
        }

        List<Map<String, String>> questions = new ArrayList<Map<String, String>>();
        for (Object result : trans.exec()) {
            Map<String, String> question = (Map<String, String>) result;
            if (question != null && question.size() > 0) {
                questions.add(question);
            }
        }
        return questions;
    }

    public static Map<String, String> getQuestionById(Jedis conn, long qid, long uid) {

        if (!conn.exists("qviews:" + qid + ":" + uid)) {
            Pipeline pipeline = conn.pipelined();
            pipeline.sadd("qviews:" + qid + ":" + uid, String.valueOf(uid));
            pipeline.expire("qviews:" + qid + ":" + uid, 15 * 60);
            pipeline.hincrBy("question:" + qid, "views", 1);
            pipeline.sync();
        }

        Transaction trans = conn.multi();
        trans.hgetAll("question:" + qid);

        List<Object> result = trans.exec();
        Map<String, String> question = (Map<String, String>) result.get(0);

        return question;
    }

    public static long answerQuestion(Jedis conn, long id, long uid, String answer, String username) {
        boolean isCorrect = false;

        Transaction trans = conn.multi();
        trans.incr("answer:id:");

        List<Object> response = trans.exec();
        long answerId = (Long) response.get(0);
        long now = System.currentTimeMillis();

        Map<String, String> data = new HashMap<>();
        data.put("aid", String.valueOf(answerId));
        data.put("answer", answer);
        data.put("uid", String.valueOf(uid));
        data.put("username", username);
        data.put("isCorrect", String.valueOf(isCorrect));
        data.put("date", String.valueOf(now));

        trans = conn.multi();
        trans.zadd("questiona:id:" + id, now, String.valueOf(answerId));
        trans.hmset("answer:" + answerId, data);
        trans.exec();

        return answerId;
    }

    public static boolean deleteAnswer(Jedis conn, long id, long answId, long uid, long qid) {
        if (!String.valueOf(uid).equals(conn.hget("answer:" + answId, "uid"))) {
            return false;
        }

        conn.zrem("questiona:id:" + id, String.valueOf(answId));

        if (conn.hget("answer:" + answId, "isCorrect").equals("true")) {
            Transaction trans = conn.multi();
            trans.hincrBy("user:" + uid, "ranswers", -1);
            trans.hincrBy("user:" + uid, "rep", -3);
            trans.zrem("user:ranswers:" + uid, String.valueOf(qid));
            trans.exec();
        }

        return true;
    }

    public static long createUser(Jedis conn, String login, String name, String secondName, String email, String countryCode, String phoneNumber, String profilePic) {

        String llogin = login.toLowerCase();
        String lock = acquireLockWithTimeout(conn, "user:" + llogin, 10, 1);
        if (lock == null) {
            return -1;
        }

        if (conn.hget("user:", llogin) != null) {
            return -1;
        }

        long id = conn.incr("user:id:");
        Transaction trans = conn.multi();
        trans.hset("user:", llogin, String.valueOf(id));
        Map<String, String> values = new HashMap<String, String>();
        values.put("login", login);
        values.put("id", String.valueOf(id));
        values.put("name", name);
        values.put("sname", secondName);
        values.put("email", email);
        values.put("countrycode", countryCode);
        values.put("phoneNumber", phoneNumber);
        values.put("profilePic", profilePic);
        values.put("recos", "0");
        values.put("questions", "0");
        values.put("ranswers", "0");
        values.put("rep", "0");
        values.put("followers", "0");
        values.put("following", "0");
        values.put("signup", String.valueOf(System.currentTimeMillis()));
        trans.hmset("user:" + id, values);
        trans.exec();
        releaseLock(conn, "user:" + llogin, lock);
        // get bages
        return id;
    }

    public static long createUser(Jedis conn, String login, String email, String password, String profilePic) {

        String llogin = login.toLowerCase();
        String lock = acquireLockWithTimeout(conn, "user:" + llogin, 5, 1);
        if (lock == null) {
            return -1;
        }

        if (conn.hget("user:", llogin) != null) {
            return -2;
        }

        byte[] salt = SecurePassword.getNextSalt();
        byte[] hashed = SecurePassword.hash(password.toCharArray(), salt);

        long id = conn.incr("user:id:");
        Transaction trans = conn.multi();
        trans.hset("useremail:", email, String.valueOf(id));
        trans.hset("user:", llogin, String.valueOf(id));
        Map<String, String> values = new HashMap<String, String>();
        values.put("login", llogin);
        values.put("id", String.valueOf(id));
        values.put("email", email);
        values.put("profilePic", profilePic);
        values.put("hpass", Base64.encode(hashed));
        values.put("salt", Base64.encode(salt));
        values.put("recos", "0");
        values.put("questions", "0");
        values.put("ranswers", "0");
        values.put("rep", "0");
        values.put("followers", "0");
        values.put("following", "0");
        values.put("signup", String.valueOf(System.currentTimeMillis()));
        trans.hmset("user:" + id, values);
        trans.exec();
        addName(llogin, conn);
        releaseLock(conn, "user:" + llogin, lock);
        return id;
    }

    public static UserPage getUserPage(Jedis conn, String uid, String ruid, int p) {

        UserPage userPage = new UserPage();
        Transaction trans = conn.multi();
        trans.hgetAll("user:" + uid);

        for (Object result : trans.exec()) {
            Map<String, String> ui = (Map<String, String>) result;
            userPage.setUsername(ui.get("login"));
            userPage.setId(Long.parseLong(uid));
            userPage.setProfilePic(ui.get("profilePic"));
            userPage.setFollowers(ui.get("followers"));
            userPage.setFollowing(ui.get("following"));
            userPage.setQuestions(ui.get("questions"));
            userPage.setrAnswers(ui.get("rAnswers"));
            userPage.setRecosCount(ui.get("recos"));
            userPage.setReputation(ui.get("reputation"));

            if (uid.equals(ruid)) {
                userPage.setIsFollowing(null);
            } else {
                Set<String> following = conn.zrange("following:" + ruid, 0, -1);
                if (following.contains(uid)) {
                    userPage.setIsFollowing(true);
                } else {
                    userPage.setIsFollowing(false);
                }
            }
        }

        userPage.setFeedItems(getProfilePosts(conn, Long.parseLong(uid), p, 7, Long.parseLong(ruid)));

        return userPage;

    }

    public static FeedItem getMovieTvShowPosts(Jedis conn, long uid, long ruid, Map<String, String> post) {

        FeedItem feedItem = new FeedItem();

        feedItem.setUserId(Long.parseLong(post.get("uid")));
        feedItem.setPostId(Long.parseLong(post.get("id")));
        feedItem.setUsername(post.get("username"));
        feedItem.setItemName(post.get("iname"));
        feedItem.setType(post.get("type"));
        feedItem.setGenre(post.get("genre"));
        feedItem.setInfo(post.get("info"));
        feedItem.setCommentCount(Integer.parseInt(post.get("comments")));
        feedItem.setLikes(Long.parseLong(post.get("likes")));
        feedItem.setPoster_url(post.get("purl"));
        feedItem.setCitation(post.get("cit"));
        feedItem.setFeedback(post.get("feedback"));
        feedItem.setItemId(Long.parseLong(post.get("itemid")));
        feedItem.setTimestamp(post.get("date"));


        List<Comment> commentList = new ArrayList<>();
        List<Map<String, String>> comments = getComments(conn, feedItem.getPostId(), 0, 3, false);
        for (Map<String, String> c : comments) {
            Comment comment = new Comment();
            comment.setCid(Long.parseLong(c.get("cid")));
            comment.setComment(c.get("comment"));
            comment.setUid(Long.parseLong(c.get("uid")));
            comment.setUsername(c.get("uname"));
            comment.setTimestamp(c.get("date"));
            comment.setProfilePic(c.get("profilePic"));
            commentList.add(comment);
        }
        feedItem.setComments(commentList);

        Pipeline p = conn.pipelined();
        Response<Double> score = null;
        if (ruid == 0) {
            score = p.zscore("postl:id:" + feedItem.getPostId(), String.valueOf(uid));
        } else {
            score = p.zscore("postl:id:" + feedItem.getPostId(), String.valueOf(ruid));
        }
        Response<Long> likesnum = p.zcard("postl:id:" + feedItem.getPostId());
        Response<String> profilePic = p.hget("user:" + uid, "profilePic");
        p.sync();

        feedItem.setProfilePic(profilePic.get());

        if (score.get() != null)
            feedItem.setLiked(true);

        if (likesnum.get() < 4) {
            Set<String> likers = conn.zrange("postl:id:" + feedItem.getPostId(), 0, -1);
            p = conn.pipelined();
            for (String s : likers) {
                p.hget("user:" + s, "login");
            }
            List<Object> usernames = p.syncAndReturnAll();
            List<String> likerstoGo = new ArrayList<>();
            for (Object u : usernames) {
                likerstoGo.add((String) u);
            }
            feedItem.setLikers(likerstoGo);
        }

        return feedItem;

    }

    private static FeedItem getYvideoPosts(Jedis conn, long uid, long ruid, Map<String, String> post){
        FeedItem feedItem = new FeedItem();

        feedItem.setUserId(Long.parseLong(post.get("uid")));
        feedItem.setPostId(Long.parseLong(post.get("id")));
        feedItem.setUsername(post.get("username"));
        feedItem.setItemName(post.get("iname"));
        feedItem.setType(post.get("type"));
        feedItem.setGenre(post.get("genre"));
        feedItem.setGenreId(Long.parseLong(post.get("genreid")));
        feedItem.setChannelId(Long.parseLong(post.get("chid")));
        feedItem.setChannelName(post.get("channel"));
        feedItem.setInfo(post.get("info"));
        feedItem.setCommentCount(Integer.parseInt(post.get("comments")));
        feedItem.setLikes(Long.parseLong(post.get("likes")));
        feedItem.setPoster_url(post.get("purl"));
        feedItem.setCitation(post.get("cit"));
        feedItem.setFeedback(post.get("feedback"));
        feedItem.setItemId(Long.parseLong(post.get("itemid")));
        feedItem.setTimestamp(post.get("date"));


        List<Comment> commentList = new ArrayList<>();
        List<Map<String, String>> comments = getComments(conn, feedItem.getPostId(), 0, 3, false);
        for (Map<String, String> c : comments) {
            Comment comment = new Comment();
            comment.setCid(Long.parseLong(c.get("cid")));
            comment.setComment(c.get("comment"));
            comment.setUid(Long.parseLong(c.get("uid")));
            comment.setUsername(c.get("uname"));
            comment.setTimestamp(c.get("date"));
            comment.setProfilePic(c.get("profilePic"));
            commentList.add(comment);
        }
        feedItem.setComments(commentList);

        Pipeline p = conn.pipelined();
        Response<Double> score = null;
        if (ruid == 0) {
            score = p.zscore("postl:id:" + feedItem.getPostId(), String.valueOf(uid));
        } else {
            score = p.zscore("postl:id:" + feedItem.getPostId(), String.valueOf(ruid));
        }
        Response<Long> likesnum = p.zcard("postl:id:" + feedItem.getPostId());
        Response<String> profilePic = p.hget("user:" + uid, "profilePic");
        p.sync();

        feedItem.setProfilePic(profilePic.get());

        if (score.get() != null)
            feedItem.setLiked(true);

        if (likesnum.get() < 4) {
            Set<String> likers = conn.zrange("postl:id:" + feedItem.getPostId(), 0, -1);
            p = conn.pipelined();
            for (String s : likers) {
                p.hget("user:" + s, "login");
            }
            List<Object> usernames = p.syncAndReturnAll();
            List<String> likerstoGo = new ArrayList<>();
            for (Object u : usernames) {
                likerstoGo.add((String) u);
            }
            feedItem.setLikers(likerstoGo);
        }

        return feedItem;
    }

    public static void addName(final String name, Jedis conn) {
        Pipeline p = conn.pipelined();
        String namen = name + "*";
        p.zadd("namesearch", 0, namen);
        for (int i = 1; i < namen.length(); i++) {
            if (i < 3) {
                continue;
            } else {
                p.zadd("namesearch", 0, namen.substring(0, i));
            }
        }
        p.sync();
    }

    public static List<String> autoComplete(final String prefix, final int count, Jedis redis) {
        if (null == prefix) {
            return Collections.emptyList();
        }

        int prefixLength = prefix.length();
        Long start = redis.zrank("namesearch", prefix);

        if (start == null || prefixLength == 0) {
            return Collections.emptyList();
        }
        List<String> results = new ArrayList<String>();
        int found = 0, rangeLength = 50, maxNeeded = count;
        while (found < maxNeeded) {
            Set<String> rangeResults = redis.zrange("namesearch", start, start + rangeLength - 1);
            start += rangeLength;
            if (rangeResults.isEmpty()) {
                break;
            }
            for (final String entry : rangeResults) {
                int minLength = Math.min(entry.length(), prefixLength);
                if (!entry.substring(0, minLength).equalsIgnoreCase(prefix.substring(0, minLength))) {
                    maxNeeded = results.size();
                    break;
                }
                if (entry.endsWith("*") && results.size() < maxNeeded) {
                    results.add(entry.substring(0, entry.length() - 1));
                }
            }
        }
        return results;
    }

    public static boolean deletePost(Jedis conn, String uid, String postId) {
        String key = "post:" + postId;
        String lock = acquireLockWithTimeout(conn, key, 1, 10);
        if (lock == null) {
            return false;
        }

        try {
            if (!uid.equals(conn.hget(key, "uid"))) {
                return false;
            }

            Set<String> comments = conn.zrange("postc:id:" + postId, 0, -1);

            Transaction trans = conn.multi();
            trans.zrem("profile:" + uid, postId);
            trans.zrem("home:" + uid, postId);
            trans.del("postc:id:" + postId);
            trans.del("post:" + postId);
            trans.del("postl:id:" + postId);

            trans.hincrBy("post:" + postId, "comments", -1);
            trans.hincrBy("user:" + uid, "recos", -1);
            trans.exec();

            Pipeline p = conn.pipelined();
            for (String s : comments) {
                p.del("comment:" + s);
            }
            p.sync();

            cleanTimelines(conn, Long.parseLong(uid), Long.parseLong(postId), 0, false);

            return true;
        } finally {
            releaseLock(conn, key, lock);
        }
    }

    public static long createComment(Jedis conn, long uid, String username, String postId, String comment) throws UnsupportedEncodingException {

        Transaction trans = conn.multi();
        trans.incr("comment:id:");

        List<Object> response = trans.exec();
        long commentId = (Long) response.get(0);
        long now = System.currentTimeMillis();

        Map<String, String> data = new HashMap<String, String>();
        data.put("cid", String.valueOf(commentId));
        data.put("comment", comment);
//        data.put("comment", new String(comment.getBytes(Charset.forName("UTF-8")), "UTF-8"));
//        data.put("comment", new String(comment.getBytes(Charset.forName("windows-1251")), "UTF-8"));
        data.put("uid", String.valueOf(uid));
        data.put("uname", username);
        data.put("date", String.valueOf(now));

        trans = conn.multi();
        trans.zadd("postc:id:" + postId, now, String.valueOf(commentId));
        trans.hmset("comment:" + commentId, data);
        trans.hincrBy("post:" + postId, "comments", 1);
        trans.hget("post:" + postId, "date");

        List<Object> result = trans.exec();

        String postTime = (String) result.get(result.size() - 1);

        if (postTime != null) {
            syndicateStatus(conn, uid, Long.parseLong(postId), Long.parseLong(postTime), 0);
        }

        return commentId;
    }

    public static boolean deleteComment(Jedis conn, String uid, String postId, String commentId) {

        if (!uid.equals(conn.hget("comment:" + commentId, "uid"))) {
            return false;
        }

        if (conn.zscore("postc:id:" + postId, commentId) == null) {
            return false;
        }

        Transaction trans = conn.multi();
        trans.zrem("postc:id:" + postId, commentId);
        trans.hincrBy("post:" + postId, "comments", -1);
        trans.exec();

        return true;
    }

    @SuppressWarnings("unchecked")
    public static List<Map<String, String>> getComments(Jedis conn, long postId, int page, int count, boolean all) {
        Set<String> commentIds = null;

        if (all) {
            commentIds = conn.zrange("postc:id:" + postId, 0, -1);
        } else {
            commentIds = conn.zrange("postc:id:" + postId, (page - 1) * count, page * count - 1);
        }

        Transaction trans = conn.multi();
        for (String id : commentIds) {
            trans.hgetAll("comment:" + id);
        }

        List<Map<String, String>> comments = new ArrayList<>();
        for (Object result : trans.exec()) {
            Map<String, String> comment = (Map<String, String>) result;
            if (comment != null && comment.size() > 0) {
                String profilePic = conn.hget("user:" + comment.get("uid"), "profilePic");
                comment.put("profilePic", profilePic);
                comments.add(comment);
            }
        }
        return comments;
    }

    @SuppressWarnings("unchecked")
    public static List<FeedItem> getHomePosts(Jedis conn, long uid, int page, int count) throws UnsupportedEncodingException {
        Set<String> postIds = conn.zrevrange("home:" + uid, (page - 1) * count, page * count - 1);

        Transaction trans = conn.multi();
        for (String id : postIds) {
            trans.hgetAll("post:" + id);
        }

        List<FeedItem> feedItems = new ArrayList<>();

        for (Object result : trans.exec()) {
            Map<String, String> post = (Map<String, String>) result;

            if (post != null && post.size() > 0) {

                FeedItem feedItem = null;
                String type = post.get("type");
                switch (type) {
                    case "book":
                        feedItem = (FeedItem) getBookPosts(conn, uid, 0, post);
                        break;
                    case "movie":
                        feedItem = (FeedItem) getMovieTvShowPosts(conn, uid, 0, post);
                        break;
                    case "tvshow":
                        feedItem = (FeedItem) getMovieTvShowPosts(conn, uid, 0, post);
                        break;
                    case "place":
                        break;
                    case "music":
                        feedItem = (FeedItem) getMusicPosts(conn, uid, 0, post);
                        break;
                    case "yvideo":
                        feedItem = (FeedItem) getYvideoPosts(conn, uid, 0, post);
                        break;
                }
                feedItems.add(feedItem);
            }
        }
        return feedItems;
    }

    public static FeedItem getMusicPosts (Jedis conn, long uid, long ruid, Map<String, String> post){
        FeedItem feedItem = new FeedItem();
        feedItem.setUserId(Long.parseLong(post.get("uid")));
        feedItem.setUsername(post.get("username"));
        feedItem.setItemName(post.get("iname"));
        feedItem.setType(post.get("type"));
        feedItem.setGenre(post.get("genre"));
        feedItem.setInfo(post.get("info"));
        feedItem.setCommentCount(Integer.parseInt(post.get("comments")));
        feedItem.setLikes(Long.parseLong(post.get("likes")));
        feedItem.setPoster_url(post.get("purl"));
        feedItem.setFeedback(post.get("feedback"));
        feedItem.setItemId(Long.parseLong(post.get("itemid")));
        feedItem.setTimestamp(post.get("date"));
        feedItem.setPostId(Long.parseLong(post.get("id")));

        List<Comment> commentList = new ArrayList<>();
        List<Map<String, String>> comments = getComments(conn, feedItem.getPostId(), 0, 3, false);
        for (Map<String, String> c : comments) {
            Comment comment = new Comment();
            comment.setCid(Long.parseLong(c.get("cid")));
            comment.setComment(c.get("comment"));
            comment.setUid(Long.parseLong(c.get("uid")));
            comment.setUsername(c.get("uname"));
            comment.setTimestamp(c.get("date"));
            comment.setProfilePic(c.get("profilePic"));
            commentList.add(comment);
        }
        feedItem.setComments(commentList);

        Pipeline p = conn.pipelined();

        Response<Double> score = null;
        if (ruid == 0) {
            score = p.zscore("postl:id:" + feedItem.getPostId(), String.valueOf(uid));
        } else {
            score = p.zscore("postl:id:" + feedItem.getPostId(), String.valueOf(ruid));
        }
        Response<Long> likesnum = p.zcard("postl:id:" + feedItem.getPostId());
        Response<String> profilePic = p.hget("user:" + uid, "profilePic");
        p.sync();

        feedItem.setProfilePic(profilePic.get());

        if (score.get() != null)
            feedItem.setLiked(true);

        if (likesnum.get() < 4) {
            Set<String> likers = conn.zrange("postl:id:" + feedItem.getPostId(), 0, -1);
            p = conn.pipelined();
            for (String s : likers) {
                p.hget("user:" + s, "login");
            }
            List<Object> usernames = p.syncAndReturnAll();
            List<String> likerstoGo = new ArrayList<>();
            for (Object u : usernames) {
                likerstoGo.add((String) u);
            }
            feedItem.setLikers(likerstoGo);
        }

        return feedItem;

    }

    public static FeedItem getBookPosts(Jedis conn, long uid, long ruid, Map<String, String> post) {

        FeedItem feedItem = new FeedItem();
        feedItem.setUserId(Long.parseLong(post.get("uid")));
        feedItem.setUsername(post.get("username"));
        feedItem.setItemName(post.get("iname"));
        feedItem.setType(post.get("type"));
        feedItem.setGenre(post.get("genre"));
        feedItem.setInfo(post.get("author"));
        feedItem.setCommentCount(Integer.parseInt(post.get("comments")));
        feedItem.setLikes(Long.parseLong(post.get("likes")));
        feedItem.setPoster_url(post.get("purl"));
        feedItem.setCitation(post.get("cit"));
        feedItem.setFeedback(post.get("feedback"));
        feedItem.setItemId(Long.parseLong(post.get("itemid")));
        feedItem.setTimestamp(post.get("date"));
        feedItem.setPostId(Long.parseLong(post.get("id")));

        List<Comment> commentList = new ArrayList<>();
        List<Map<String, String>> comments = getComments(conn, feedItem.getPostId(), 0, 3, false);
        for (Map<String, String> c : comments) {
            Comment comment = new Comment();
            comment.setCid(Long.parseLong(c.get("cid")));
            comment.setComment(c.get("comment"));
            comment.setUid(Long.parseLong(c.get("uid")));
            comment.setUsername(c.get("uname"));
            comment.setTimestamp(c.get("date"));
            comment.setProfilePic(c.get("profilePic"));
            commentList.add(comment);
        }
        feedItem.setComments(commentList);

        Pipeline p = conn.pipelined();

        Response<Double> score = null;
        if (ruid == 0) {
            score = p.zscore("postl:id:" + feedItem.getPostId(), String.valueOf(uid));
        } else {
            score = p.zscore("postl:id:" + feedItem.getPostId(), String.valueOf(ruid));
        }
        Response<Long> likesnum = p.zcard("postl:id:" + feedItem.getPostId());
        Response<String> profilePic = p.hget("user:" + uid, "profilePic");
        p.sync();

        feedItem.setProfilePic(profilePic.get());

        if (score.get() != null)
            feedItem.setLiked(true);

        if (likesnum.get() < 4) {
            Set<String> likers = conn.zrange("postl:id:" + feedItem.getPostId(), 0, -1);
            p = conn.pipelined();
            for (String s : likers) {
                p.hget("user:" + s, "login");
            }
            List<Object> usernames = p.syncAndReturnAll();
            List<String> likerstoGo = new ArrayList<>();
            for (Object u : usernames) {
                likerstoGo.add((String) u);
            }
            feedItem.setLikers(likerstoGo);
        }

        return feedItem;

    }

    @SuppressWarnings("unchecked")
    public static List<FeedItem> getProfilePosts(Jedis conn, long uid, int page, int count, long ruid) {
        Set<String> postIds = conn.zrevrange("profile:" + uid, (page - 1) * count, page * count - 1);

        Transaction trans = conn.multi();
        for (String id : postIds) {
            trans.hgetAll("post:" + id);
        }

        List<FeedItem> feedItems = new ArrayList<>();

        for (Object result : trans.exec()) {
            Map<String, String> post = (Map<String, String>) result;

            if (post != null && post.size() > 0) {
                FeedItem feedItem = null;
                String type = post.get("type");
                switch (type) {
                    case "book":
                        feedItem = (FeedItem) getBookPosts(conn, uid, ruid, post);
                        break;
                    case "movie":
                        feedItem = (FeedItem) getMovieTvShowPosts(conn, uid, ruid, post);
                        break;
                    case "tvshow":
                        feedItem = (FeedItem) getMovieTvShowPosts(conn, uid, ruid, post);
                        break;
                    case "place":
                        break;
                    case "music":
                        feedItem = (FeedItem) getMusicPosts(conn, uid, ruid, post);
                        break;
                    case "yvideo":
                        feedItem = (FeedItem) getYvideoPosts(conn, uid, 0, post);
                        break;
                }

                feedItems.add(feedItem);
            }
        }

        return feedItems;
    }

    @SuppressWarnings("unchecked")
    public static long likePost(Jedis conn, long postId, long uid) {
        String postLikes = "postl:id:" + postId;
        if (conn.zscore(postLikes, String.valueOf(uid)) != null) {
            return -1;
        }

        long now = System.currentTimeMillis();

        Transaction trans = conn.multi();
        trans.zadd(postLikes, now, String.valueOf(uid));
        trans.zcard(postLikes);

        List<Object> response = trans.exec();
        long likes = (Long) response.get(1);

        trans = conn.multi();
        trans.hset("post:" + postId, "likes", String.valueOf(likes));
        trans.hget("post:" + postId, "date");
        List<Object> result = trans.exec();

        String postTime = (String) result.get(1);

        if (postTime != null) {
            syndicateStatus(conn, uid, postId, Long.parseLong(postTime), 0);
        }

        return likes;
    }

    @SuppressWarnings("unchecked")
    public static boolean unlikePost(Jedis conn, String postId, String uid) {

        if (conn.zscore("postl:id:" + postId, uid) == null) {
            return false;
        }

        Transaction trans = conn.multi();
        trans.zrem("postl:id:" + postId, uid);
        trans.hincrBy("post:" + postId, "likes", -1);
        trans.exec();

        cleanTimelines(conn, Long.parseLong(uid), Long.parseLong(postId), 0, false);

        return true;
    }

    @SuppressWarnings("unchecked")
    public static boolean followUser(Jedis conn, long uid, long otherUid) {
        String fkey1 = "following:" + uid;
        String fkey2 = "followers:" + otherUid;

        if (uid == otherUid) {
            return false;
        }

        if (conn.zscore(fkey1, String.valueOf(otherUid)) != null) {
            return false;
        }

        long now = System.currentTimeMillis();

        Transaction trans = conn.multi();
        trans.zadd(fkey1, now, String.valueOf(otherUid));
        trans.zadd(fkey2, now, String.valueOf(uid));
        trans.zcard(fkey1);
        trans.zcard(fkey2);
        trans.zrevrangeWithScores("profile:" + otherUid, 0, 7);

        List<Object> response = trans.exec();
        long following = (Long) response.get(response.size() - 3);
        long followers = (Long) response.get(response.size() - 2);
        Set<Tuple> posts = (Set<Tuple>) response.get(response.size() - 1);

        trans = conn.multi();
        trans.hset("user:" + uid, "following", String.valueOf(following));
        trans.hset("user:" + otherUid, "followers", String.valueOf(followers));

        if (posts.size() > 0) {
            for (Tuple post : posts) {
                trans.zadd("home:" + uid, post.getScore(), post.getElement());
            }
        }
//        trans.zremrangeByRank("home:" + uid, 0, 0 - HOME_TIMELINE_SIZE - 1);
        trans.exec();

        return true;
    }

    @SuppressWarnings("unchecked")
    public static boolean unfollowUser(Jedis conn, long uid, long otherUid) {
        String fkey1 = "following:" + uid;
        String fkey2 = "followers:" + otherUid;

        if (conn.zscore(fkey1, String.valueOf(otherUid)) == null) {
            return false;
        }

        Transaction trans = conn.multi();
        trans.zrem(fkey1, String.valueOf(otherUid));
        trans.zrem(fkey2, String.valueOf(uid));
        trans.zcard(fkey1);
        trans.zcard(fkey2);
        trans.zrevrange("profile:" + otherUid, 0, HOME_TIMELINE_SIZE - 1);

        List<Object> response = trans.exec();
        long following = (Long) response.get(response.size() - 3);
        long followers = (Long) response.get(response.size() - 2);
        Set<String> posts = (Set<String>) response.get(response.size() - 1);

        trans = conn.multi();
        trans.hset("user:" + uid, "following", String.valueOf(following));
        trans.hset("user:" + otherUid, "followers", String.valueOf(followers));
        if (posts.size() > 0) {
            for (String post : posts) {
                trans.zrem("home:" + uid, post);
            }
        }

        trans.exec();
        return true;
    }

    public static void syndicateStatus(Jedis conn, long uid, long postId, long postTime, double start) {
        Set<Tuple> followers = conn.zrangeByScoreWithScores("followers:" + uid, String.valueOf(start), "inf", 0, POSTS_PER_PASS);

        Transaction trans = conn.multi();
        for (Tuple tuple : followers) {
            String follower = tuple.getElement();
            start = tuple.getScore();
            trans.zadd("home:" + follower, postTime, String.valueOf(postId));
//            trans.zrange("home:" + follower, 0, -1);
            trans.zremrangeByRank("home:" + follower, 0, 0 - HOME_TIMELINE_SIZE - 1);
        }
        trans.exec();

        if (followers.size() >= POSTS_PER_PASS) {
            try {
                Method method = RedisManager.class.getDeclaredMethod(
                        "syndicateStatus", Jedis.class, Long.TYPE, Long.TYPE, Double.TYPE);
                executeLater("default", method, uid, postId, postTime, start);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
        }

    }

    public static void executeLater(String queue, Method method, Object... args) {
        MethodThread thread = new MethodThread(RedisManager.class, method, args);
        thread.start();
    }

    public static class MethodThread extends Thread {
        private Object instance;
        private Method method;
        private Object[] args;

        public MethodThread(Object instance, Method method, Object... args) {
            this.instance = instance;
            this.method = method;
            this.args = args;
        }

        public void run() {
            Jedis conn = new Jedis("localhost");
            conn.select(15);

            Object[] args = new Object[this.args.length + 1];
            System.arraycopy(this.args, 0, args, 1, this.args.length);
            args[0] = conn;

            try {
                method.invoke(instance, args);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void cleanTimelines(Jedis conn, long uid, long statusId) {
        cleanTimelines(conn, uid, statusId, 0, false);
    }

    public static void cleanTimelines(Jedis conn, long uid, long postId, double start, boolean onLists) {
        String key = "followers:" + uid;
        String base = "home:";
        if (onLists) {
            key = "list:out:" + uid;
            base = "list:posts:";
        }
        Set<Tuple> followers = conn.zrangeByScoreWithScores(
                key, String.valueOf(start), "inf", 0, POSTS_PER_PASS);

        Transaction trans = conn.multi();
        for (Tuple tuple : followers) {
            start = tuple.getScore();
            String follower = tuple.getElement();
            trans.zrem(base + follower, String.valueOf(postId));
        }
        trans.exec();

        Method method = null;
        try {
            method = RedisManager.class.getDeclaredMethod(
                    "cleanTimelines", Jedis.class,
                    Long.TYPE, Long.TYPE, Double.TYPE, Boolean.TYPE);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        if (followers.size() >= POSTS_PER_PASS) {
            executeLater("default", method, uid, postId, start, onLists);

        } else if (!onLists) {
            executeLater("default", method, uid, postId, 0, true);
        }
    }

    public static void refillTimeline(Jedis conn, String incoming, String timeline) {
        refillTimeline(conn, incoming, timeline, 0);
    }

    @SuppressWarnings("unchecked")
    public static void refillTimeline(Jedis conn, String incoming, String timeline, double start) {
        if (start == 0 && conn.zcard(timeline) >= 750) {
            return;
        }

        Set<Tuple> users = conn.zrangeByScoreWithScores(
                incoming, String.valueOf(start), "inf", 0, REFILL_USERS_STEP);

        Pipeline pipeline = conn.pipelined();
        for (Tuple tuple : users) {
            String uid = tuple.getElement();
            start = tuple.getScore();
            pipeline.zrevrangeWithScores(
                    "profile:" + uid, 0, HOME_TIMELINE_SIZE - 1);
        }

        List<Object> response = pipeline.syncAndReturnAll();
        List<Tuple> messages = new ArrayList<Tuple>();
        for (Object results : response) {
            messages.addAll((Set<Tuple>) results);
        }

        Collections.sort(messages);
        messages = messages.subList(0, HOME_TIMELINE_SIZE);

        Transaction trans = conn.multi();
        if (messages.size() > 0) {
            for (Tuple tuple : messages) {
                trans.zadd(timeline, tuple.getScore(), tuple.getElement());
            }
        }
        trans.zremrangeByRank(timeline, 0, 0 - HOME_TIMELINE_SIZE - 1);
        trans.exec();

        if (users.size() >= REFILL_USERS_STEP) {
            try {
                Method method = RedisManager.class.getDeclaredMethod(
                        "refillTimeline", Jedis.class, String.class, String.class, Double.TYPE);
                executeLater("default", method, incoming, timeline, start);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static boolean releaseLock(Jedis conn, String lockName, String identifier) {
        lockName = "lock:" + lockName;
        while (true) {
            conn.watch(lockName);
            if (identifier.equals(conn.get(lockName))) {
                Transaction trans = conn.multi();
                trans.del(lockName);
                List<Object> result = trans.exec();
                // null response indicates that the transaction was aborted due
                // to the watched key changing
                if (result == null) {
                    continue;
                }
                return true;
            }
            conn.unwatch();
            break;
        }
        return false;
    }

    public static String acquireLockWithTimeout(Jedis conn, String lockName, int acquireTimeout, int lockTimeout) {
        String id = UUID.randomUUID().toString();
        lockName = "lock:" + lockName;

        long end = System.currentTimeMillis() + (acquireTimeout * 1000);
        while (System.currentTimeMillis() < end) {
            if (conn.setnx(lockName, id) >= 1) {
                conn.expire(lockName, lockTimeout);
                return id;
            } else if (conn.ttl(lockName) <= 0) {
                conn.expire(lockName, lockTimeout);
            }

            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return id;
    }


}
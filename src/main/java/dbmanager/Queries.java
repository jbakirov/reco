package dbmanager;

import models.Item;
import redis.RedisManager;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 * Created by Baka on 07.08.2015.
 */
public class Queries {

    static DbManager dbManager = new DbManager();

    public static Properties getQueries() throws SQLException {
        Properties properties = null;
        try {
            properties = new Properties();
            properties.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("query.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return properties;
    }

    public static String getQuery(String query) throws SQLException {
        return getQueries().getProperty(query);
    }

    public static Long addNewItemRu(Item item) throws SQLException {
        String query = "";
        String type = item.getType();


        switch (type) {
            case "book":
                query = Queries.getQuery("addNewBookRu");
                break;
            case "movie":
                query = Queries.getQuery("addNewMovieRu");
                break;
            case "tvshow":
                query = Queries.getQuery("addNewTvShowRu");
                break;
            case "music":
                break;
            case "place":
                break;
            case "yvideo":
                break;
        }


        Long res = null;
        Connection connection = null;
        try {
            connection = dbManager.getConnectionPool().getConnection();
            try {
                PreparedStatement preparedStatement = connection.prepareStatement(query);
                preparedStatement.setString(1, item.getName());
                preparedStatement.setString(2, item.getInfo());
                preparedStatement.setString(3, item.getPoster_url());
                preparedStatement.setString(4, item.getGenres());
                try {
                    ResultSet resultSet = preparedStatement.executeQuery();
                    try {
                        while (resultSet.next()) {
                            res = resultSet.getLong(1);
                        }
                    } finally {
                        resultSet.close();
                    }
                } finally {
                    preparedStatement.close();
                }
            } finally {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return res;
    }

    public static Long addNewItem(Item item) throws SQLException {

        String query = "";
        String type = item.getType();

        switch (type) {
            case "book":
                query = Queries.getQuery("addNewBook");
                break;
            case "movie":
                query = Queries.getQuery("addNewMovie");
                break;
            case "tvshow":
                query = Queries.getQuery("addNewTvShow");
                break;
            case "music":
                query = Queries.getQuery("addNewMusic");
                break;
            case "place":
                break;
            case "yvideo":
                return Long.parseLong(addYoutubeVideo(item));
        }

        Long res = null;
        Connection connection = null;
        try {
            connection = dbManager.getConnectionPool().getConnection();
            try {
                connection.setAutoCommit(false);
                PreparedStatement preparedStatement = connection.prepareStatement(query);


                preparedStatement.setString(1, item.getName());
                preparedStatement.setString(2, item.getInfo());
                preparedStatement.setString(3, item.getPoster_url());
                preparedStatement.setString(4, item.getGenres());


                try {
                    preparedStatement.executeUpdate();

                    preparedStatement = connection.prepareStatement(Queries.getQuery("getLastAddedId"));
                    ResultSet resultSet = preparedStatement.executeQuery();
                    connection.commit();

                    try {
                        while (resultSet.next()) {
                            res = resultSet.getLong(1);
                        }
                    } finally {
                        resultSet.close();
                    }
                } finally {
                    preparedStatement.close();
                }
            } finally {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return res;
    }

    private static String addYoutubeVideo(Item item) throws SQLException {
        String query = Queries.getQuery("addNewYvideo");

        Connection connection = null;
        try {
            connection = dbManager.getConnectionPool().getConnection();
            try {
                connection.setAutoCommit(false);
                PreparedStatement preparedStatement = connection.prepareStatement(query);


                preparedStatement.setString(1, item.getVideoId());
                preparedStatement.setString(2, item.getName());
                preparedStatement.setString(3, item.getChannel());
                preparedStatement.setString(4, item.getChannelId());
                preparedStatement.setString(5, item.getPoster_url());
                preparedStatement.setString(6, item.getGenreId());
                preparedStatement.setString(7, item.getGenres());


                try {
                    preparedStatement.executeUpdate();

                } finally {
                    preparedStatement.close();
                }
            } finally {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return item.getVideoId();
    }


    public static List<Item> getBooksByGenre(String value, String lang) throws SQLException {
        String query = "";

        switch (lang) {
            case "ru":
                query = getQuery("getBooksByGenreRu");
                break;
            case "en":
                query = getQuery("getBooksByGenre");
                break;
        }

        List<Item> items = new ArrayList<>();
        Connection connection = null;
        try {
            connection = dbManager.getConnectionPool().getConnection();
            try {
                PreparedStatement preparedStatement = connection.prepareStatement(query);
                preparedStatement.setString(1, "%" + value + "%");
                try {
                    ResultSet resultSet = preparedStatement.executeQuery();
                    try {
                        Jedis conn = null;
                        conn = RedisManager.pool.getResource();
                        try {
                            String recos;
                            while (resultSet.next()) {
                                Item item = new Item();
                                item.setId(resultSet.getLong(1));
                                item.setName(resultSet.getString(2));
                                item.setInfo(resultSet.getString(3));
                                item.setPoster_url(resultSet.getString(4));
                                item.setGenres(resultSet.getString(5));

                                recos = conn.hget("book:recos:" + item.getId(), "recos");
                                item.setRecos_count(recos);
                                items.add(item);
                            }

                        } finally {
                            if (conn != null) {
                                conn.close();
                            }
                        }

                    } finally {
                        resultSet.close();
                    }
                } finally {
                    preparedStatement.close();
                }
            } finally {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return items;
    }

    public static List<Item> getMusicByGenre(String value) throws SQLException {
        String query = Queries.getQuery("getMusicByGenre");

        List<Item> items = new ArrayList<>();
        Connection connection = null;
        try {
            connection = dbManager.getConnectionPool().getConnection();
            try {
                PreparedStatement preparedStatement = connection.prepareStatement(query);
                preparedStatement.setString(1, "%" + value + "%");
                try {
                    ResultSet resultSet = preparedStatement.executeQuery();
                    try {
                        Jedis conn = null;
                        conn = RedisManager.pool.getResource();
                        try {
                            String recos;
                            while (resultSet.next()) {
                                Item item = new Item();
                                item.setId(resultSet.getLong(1));
                                item.setName(resultSet.getString(2));
                                item.setSinger(resultSet.getString(3));
                                item.setPoster_url(resultSet.getString(4));
                                item.setGenres(resultSet.getString(5));

                                recos = conn.hget("music:recos:" + item.getId(), "recos");
                                item.setRecos_count(recos);
                                items.add(item);
                            }

                        } finally {
                            if (conn != null) {
                                conn.close();
                            }
                        }

                    } finally {
                        resultSet.close();
                    }
                } finally {
                    preparedStatement.close();
                }
            } finally {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return items;
    }

    public static List<Item> getMoviesByGenre(String value, String lang) throws SQLException {
        String query = "";

        switch (lang) {
            case "ru":
                query = getQuery("getMoviesByGenreRu");
                break;
            case "en":
                query = getQuery("getMoviesByGenre");
                break;
        }

        List<Item> items = new ArrayList<>();
        Connection connection = null;
        try {
            connection = dbManager.getConnectionPool().getConnection();
            try {
                PreparedStatement preparedStatement = connection.prepareStatement(query);
                preparedStatement.setString(1, "%" + value + "%");
                try {
                    ResultSet resultSet = preparedStatement.executeQuery();
                    try {
                        Jedis conn = null;
                        conn = RedisManager.pool.getResource();
                        try {
                            String recos;
                            while (resultSet.next()) {
                                Item item = new Item();
                                item.setId(resultSet.getLong(1));
                                item.setName(resultSet.getString(2));
                                item.setInfo(resultSet.getString(3));
                                item.setPoster_url(resultSet.getString(4));
                                item.setGenres(resultSet.getString(5));

                                recos = conn.hget("movie:recos:" + item.getId(), "recos");
                                item.setRecos_count(recos);
                                items.add(item);
                            }

                        } finally {
                            if (conn != null) {
                                conn.close();
                            }
                        }

                    } finally {
                        resultSet.close();
                    }
                } finally {
                    preparedStatement.close();
                }
            } finally {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return items;
    }

    public static List<Item> getTvShowsByGenre(String value, String lang) throws SQLException {
        String query = "";

        switch (lang) {
            case "ru":
                query = getQuery("getTvShowsByGenreRu");
                break;
            case "en":
                query = getQuery("getTvShowsByGenre");
                break;
        }

        List<Item> items = new ArrayList<>();
        Connection connection = null;
        try {
            connection = dbManager.getConnectionPool().getConnection();
            try {
                PreparedStatement preparedStatement = connection.prepareStatement(query);
                preparedStatement.setString(1, "%" + value + "%");
                try {
                    ResultSet resultSet = preparedStatement.executeQuery();
                    try {
                        Jedis conn = null;
                        conn = RedisManager.pool.getResource();
                        try {
                            String recos;
                            while (resultSet.next()) {
                                Item item = new Item();
                                item.setId(resultSet.getLong(1));
                                item.setName(resultSet.getString(2));
                                item.setInfo(resultSet.getString(3));
                                item.setPoster_url(resultSet.getString(4));
                                item.setGenres(resultSet.getString(5));

                                recos = conn.hget("tvshow:recos:" + item.getId(), "recos");
                                item.setRecos_count(recos);
                                items.add(item);
                            }

                        } finally {
                            if (conn != null) {
                                conn.close();
                            }
                        }

                    } finally {
                        resultSet.close();
                    }
                } finally {
                    preparedStatement.close();
                }
            } finally {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return items;
    }

    public static List<Item> getBooks(String value, String lang) throws SQLException {
        String query = "";

        switch (lang) {
            case "ru":
                query = getQuery("getBooksRu");
                break;
            case "en":
                query = getQuery("getBooks");
                break;
        }

        List<Item> items = new ArrayList<>();
        Connection connection = null;
        try {
            connection = dbManager.getConnectionPool().getConnection();
            try {
                PreparedStatement preparedStatement = connection.prepareStatement(query);
                preparedStatement.setString(1, value + "%");
                try {
                    ResultSet resultSet = preparedStatement.executeQuery();
                    try {
                        Jedis conn = null;
                        conn = RedisManager.pool.getResource();
                        try {
                            String recos;
                            while (resultSet.next()) {
                                Item item = new Item();
                                item.setId(resultSet.getLong(1));
                                item.setName(resultSet.getString(2));
                                item.setInfo(resultSet.getString(3));
                                item.setPoster_url(resultSet.getString(4));
                                item.setGenres(resultSet.getString(5));

                                recos = conn.hget("book:recos:" + item.getId(), "recos");
                                item.setRecos_count(recos);
                                items.add(item);
                            }

                        } finally {
                            if (conn != null) {
                                conn.close();
                            }
                        }

                    } finally {
                        resultSet.close();
                    }
                } finally {
                    preparedStatement.close();
                }
            } finally {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return items;
    }

    public static List<Item> getMusic(String value) throws SQLException {
        String query = getQuery("getMusic");

        List<Item> items = new ArrayList<>();
        Connection connection = null;
        try {
            connection = dbManager.getConnectionPool().getConnection();
            try {
                PreparedStatement preparedStatement = connection.prepareStatement(query);
                preparedStatement.setString(1, value + "%");
                try {
                    ResultSet resultSet = preparedStatement.executeQuery();
                    try {
                        Jedis conn = null;
                        conn = RedisManager.pool.getResource();
                        try {
                            String recos;
                            while (resultSet.next()) {
                                Item item = new Item();
                                item.setId(resultSet.getLong(1));
                                item.setName(resultSet.getString(2));
                                item.setSinger(resultSet.getString(3));
                                item.setPoster_url(resultSet.getString(4));
                                item.setGenres(resultSet.getString(5));

                                recos = conn.hget("music:recos:" + item.getId(), "recos");
                                item.setRecos_count(recos);
                                items.add(item);
                            }

                        } finally {
                            if (conn != null) {
                                conn.close();
                            }
                        }

                    } finally {
                        resultSet.close();
                    }
                } finally {
                    preparedStatement.close();
                }
            } finally {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return items;
    }

    public static List<Item> getMovies(String value, String lang) throws SQLException {
        String query = "";

        switch (lang) {
            case "ru":
                query = getQuery("getMoviesRu");
                break;
            case "en":
                query = getQuery("getMovies");
                break;
        }

        List<Item> items = new ArrayList<>();
        Connection connection = null;
        try {
            connection = dbManager.getConnectionPool().getConnection();
            try {
                PreparedStatement preparedStatement = connection.prepareStatement(query);
                preparedStatement.setString(1, value + "%");
                try {
                    ResultSet resultSet = preparedStatement.executeQuery();
                    try {
                        Jedis conn = null;
                        conn = RedisManager.pool.getResource();
                        try {
                            String recos;
                            while (resultSet.next()) {
                                Item item = new Item();
                                item.setId(resultSet.getLong(1));
                                item.setName(resultSet.getString(2));
                                item.setInfo(resultSet.getString(3));
                                item.setPoster_url(resultSet.getString(4));
                                item.setGenres(resultSet.getString(5));

                                recos = conn.hget("movie:recos:" + item.getId(), "recos");
                                item.setRecos_count(recos);
                                items.add(item);
                            }

                        } finally {
                            if (conn != null) {
                                conn.close();
                            }
                        }

                    } finally {
                        resultSet.close();
                    }
                } finally {
                    preparedStatement.close();
                }
            } finally {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return items;
    }

    public static List<Item> getTvShows(String value, String lang) throws SQLException {
        String query = "";

        switch (lang) {
            case "ru":
                query = getQuery("getTvShowsRu");
                break;
            case "en":
                query = getQuery("getTvShows");
                break;
        }

        List<Item> items = new ArrayList<>();
        Connection connection = null;
        try {
            connection = dbManager.getConnectionPool().getConnection();
            try {
                PreparedStatement preparedStatement = connection.prepareStatement(query);
                preparedStatement.setString(1, value + "%");
                try {
                    ResultSet resultSet = preparedStatement.executeQuery();
                    try {
                        Jedis conn = null;
                        conn = RedisManager.pool.getResource();
                        try {
                            String recos;
                            while (resultSet.next()) {
                                Item item = new Item();
                                item.setId(resultSet.getLong(1));
                                item.setName(resultSet.getString(2));
                                item.setInfo(resultSet.getString(3));
                                item.setPoster_url(resultSet.getString(4));
                                item.setGenres(resultSet.getString(5));

                                recos = conn.hget("tvshow:recos:" + item.getId(), "recos");
                                item.setRecos_count(recos);
                                items.add(item);
                            }

                        } finally {
                            if (conn != null) {
                                conn.close();
                            }
                        }

                    } finally {
                        resultSet.close();
                    }
                } finally {
                    preparedStatement.close();
                }
            } finally {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return items;
    }

    public static Item getItemById(String itemId, String type) throws SQLException {
        Item item = null;
        switch (type) {
            case "book":
                item = (Item) getBookById(itemId);
                break;
            case "movie":
                item = (Item) getMovieById(itemId);
                break;
            case "tvshow":
                item = (Item) getTvshowById(itemId);
                break;
            case "music":
                item = (Item) getMusicById(itemId);
                break;
        }
        return item;
    }

    public static Item getMusicById(String musicId) throws SQLException {
        String query = getQuery("getMusicById");
        Item item = new Item();

        Connection connection = null;
        try {
            connection = dbManager.getConnectionPool().getConnection();
            try {
                PreparedStatement preparedStatement = connection.prepareStatement(query);
                preparedStatement.setString(1, musicId);
                try {
                    ResultSet resultSet = preparedStatement.executeQuery();
                    try {
                        while (resultSet.next()) {
                            item.setName(resultSet.getString(2));
                            item.setInfo(resultSet.getString(3));
                            item.setPoster_url(resultSet.getString(4));
                            item.setGenres(resultSet.getString(5));
                        }
                    } finally {
                        resultSet.close();
                    }
                } finally {
                    preparedStatement.close();
                }
            } finally {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return item;
    }

    public static Item getTvshowById(String tvshowId) throws SQLException {
        String query = getQuery("getTvshowById");
        Item item = new Item();

        Connection connection = null;
        try {
            connection = dbManager.getConnectionPool().getConnection();
            try {
                PreparedStatement preparedStatement = connection.prepareStatement(query);
                preparedStatement.setString(1, tvshowId);
                try {
                    ResultSet resultSet = preparedStatement.executeQuery();
                    try {
                        while (resultSet.next()) {
                            item.setName(resultSet.getString(2));
                            item.setInfo(resultSet.getString(3));
                            item.setPoster_url(resultSet.getString(4));
                            item.setGenres(resultSet.getString(5));
                        }
                    } finally {
                        resultSet.close();
                    }
                } finally {
                    preparedStatement.close();
                }
            } finally {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return item;
    }

    public static Item getMovieById(String movieId) throws SQLException {
        String query = getQuery("getMovieById");
        Item item = new Item();

        Connection connection = null;
        try {
            connection = dbManager.getConnectionPool().getConnection();
            try {
                PreparedStatement preparedStatement = connection.prepareStatement(query);
                preparedStatement.setString(1, movieId);
                try {
                    ResultSet resultSet = preparedStatement.executeQuery();
                    try {
                        while (resultSet.next()) {
                            item.setName(resultSet.getString(2));
                            item.setInfo(resultSet.getString(3));
                            item.setPoster_url(resultSet.getString(4));
                            item.setGenres(resultSet.getString(5));
                        }
                    } finally {
                        resultSet.close();
                    }
                } finally {
                    preparedStatement.close();
                }
            } finally {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return item;
    }

    public static List<Item> getTop10Items(Set<String> itemIds, String type) throws SQLException {
        String query = "";
        List<Item> items = new ArrayList<>();

        switch (type) {
            case "movie":
                getQuery("getMovieById");
                break;
        }

        Connection connection = null;
        try {
            connection = dbManager.getConnectionPool().getConnection();
            try {
                for (String itemId : itemIds) {
                    PreparedStatement preparedStatement = connection.prepareStatement(query);
                    preparedStatement.setString(1, itemId);

                    Item item = new Item();
                    try {
                        ResultSet resultSet = preparedStatement.executeQuery();
                        try {
                            while (resultSet.next()) {
                                item.setName(resultSet.getString(1));
                                item.setInfo(resultSet.getString(2));
                                item.setPoster_url(resultSet.getString(3));
                                item.setGenres(resultSet.getString(4));
                            }
                        } finally {
                            resultSet.close();
                        }
                    } finally {
                        preparedStatement.close();
                    }
                    items.add(item);
                }

            } finally {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return items;
    }


    public static Item getBookById(String bookId) throws SQLException {

        String query = getQuery("getBookById");

        Item item = new Item();
        Connection connection = null;
        try {
            connection = dbManager.getConnectionPool().getConnection();
            try {
                PreparedStatement preparedStatement = connection.prepareStatement(query);
                preparedStatement.setString(1, bookId);
                try {
                    ResultSet resultSet = preparedStatement.executeQuery();
                    try {
                        while (resultSet.next()) {
                            item.setName(resultSet.getString(2));
                            item.setInfo(resultSet.getString(3));
                            item.setPoster_url(resultSet.getString(4));
                            item.setGenres(resultSet.getString(5));
                        }
                    } finally {
                        resultSet.close();
                    }
                } finally {
                    preparedStatement.close();
                }
            } finally {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return item;
    }

}

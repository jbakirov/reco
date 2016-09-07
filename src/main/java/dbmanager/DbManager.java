package dbmanager;

import models.Feed;
import models.FeedItem;
import org.apache.commons.dbcp2.BasicDataSource;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Created by Baka on 07.08.2015.
 */
public class DbManager {
    private String url;
    private String driver;
    private String username;
    private String password;

    private static BasicDataSource connectionPool;

    public DbManager() {
//        FileInputStream fis = null;
        try {
            Properties properties = new Properties();
//            fis = new FileInputStream("../../resources/config.properties");
            properties.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("config.properties"));
//            properties.load(fis);
            this.url = properties.getProperty("jdbc.url");
            this.driver = properties.getProperty("jdbc.driver");
            this.username = properties.getProperty("jdbc.username");
            this.password = properties.getProperty("jdbc.password");
            Class.forName(driver);

            setConnectionPool(new BasicDataSource());
            if (username != null && password != null){
                getConnectionPool().setUsername(username);
                getConnectionPool().setPassword(password);
            }
            getConnectionPool().setDriverClassName(driver);
            getConnectionPool().setUrl(url);
            getConnectionPool().setInitialSize(1);

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static BasicDataSource getConnectionPool() {
        return connectionPool;
    }

    public static void setConnectionPool(BasicDataSource connectionPool) {
        DbManager.connectionPool = connectionPool;
    }


//    public Connection getConnection() {
//        Connection connection = null;
//        try {
//            connection = DriverManager.getConnection(url, username, password);
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
//        return connection;
//    }

//    public List<Integer> getFollowingList(int i) {
//
//        List<Integer> followingList = new ArrayList<Integer>();
//
//        try {
//            Connection connection = DriverManager.getConnection(url, username, password);
//            try {
//                PreparedStatement preparedStatement = connection.prepareStatement(Queries.getQuery("myFollowingList"));
//                preparedStatement.setInt(1, i);
//                try {
//                    ResultSet resultSet = preparedStatement.executeQuery();
//                    try {
//                        while (resultSet.next()) {
//                            followingList.add(resultSet.getInt(1));
//                        }
//                    } finally {
//                        resultSet.close();
//                    }
//                } finally {
//                    preparedStatement.close();
//                }
//            } finally {
//                connection.close();
//            }
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
//
//
//        return followingList;
//    }
//
//    public List<String> getActivitiesByUserId(int userId) {
//        FeedItem feedItem = new FeedItem();
//        try {
//            Connection connection = DriverManager.getConnection(url, username, password);
//            try {
//                PreparedStatement preparedStatement = connection.prepareStatement(Queries.getQuery("getActivitiesByUserID"));
//                preparedStatement.setInt(1, userId);
//                preparedStatement.setInt(2, userId);
//                preparedStatement.setInt(3, userId);
//                preparedStatement.setInt(4, userId);
//                preparedStatement.setInt(5, userId);
//                try {
//                    ResultSet resultSet = preparedStatement.executeQuery();
//                    try {
//                        while (resultSet.next()) {
//                            feedItem.setPostId(resultSet.getInt(1));
//                            feedItem.setType(resultSet.getString(2));
//                            feedItem.setItemId(resultSet.getInt(3));
//                            feedItem.setUserId(resultSet.getInt(4));
//                            feedItem.setDirectedToId(resultSet.getLong(5));
//                            feedItem.setFeedback(resultSet.getString(6));
//                            feedItem.setTimestamp(resultSet.getString(7));
//                        }
//                    } finally {
//                        resultSet.close();
//                    }
//                } finally {
//                    preparedStatement.close();
//                }
//
//                preparedStatement = connection.prepareStatement(Queries.getQuery(""));
//
//            } finally {
//                connection.close();
//            }
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
//
//        return null;
//    }
//
//    public Feed getMainFeed(int user_id) {
//
//        Feed feed = new Feed();
//        List<FeedItem> feedItems = new ArrayList<FeedItem>();
//        try {
//            Connection connection = DriverManager.getConnection(url, username, password);
//            try {
//                PreparedStatement preparedStatement = connection.prepareStatement(Queries.getQuery("getMainFeedByUserId"));
//                preparedStatement.setInt(1, user_id);
//                try {
//                    ResultSet resultSet = preparedStatement.executeQuery();
//                    try {
//                        while (resultSet.next()) {
//                            FeedItem feedItem = new FeedItem();
//                            feedItem.setPostId(resultSet.getInt(1));
//                            feedItem.setType(resultSet.getString(2));
//                            feedItem.setItemId(resultSet.getInt(3));
//                            feedItem.setUserId(resultSet.getInt(4));
//                            feedItem.setDirectedToId(resultSet.getLong(5));
//                            feedItem.setFeedback(resultSet.getString(6));
//                            feedItem.setTimestamp(resultSet.getString(7));
//                            feedItem.setItemName(resultSet.getString(8));
//                            feedItem.setPoster_url(resultSet.getString(9));
////                            feedItem.setRecosCount(resultSet.getInt(10));
//                            feedItem.setUsername(resultSet.getString(11));
//                            feedItem.setDirectedToUsername(resultSet.getString(12));
//                            feedItem.setLikes(resultSet.getInt(13));
////                            feedItem.setComments(resultSet.getString(14));
//                            feedItems.add(feedItem);
//                        }
//                    } finally {
//                        resultSet.close();
//                    }
//                } finally {
//                    preparedStatement.close();
//                }
//            } finally {
//                connection.close();
//            }
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
//
//        feed.setFeedItem(toArray(feedItems, FeedItem.class));
//
//        return feed;
//    }


    public <T> T[] toArray(List<T> list, Class<T> k) {
        return list.toArray((T[]) java.lang.reflect.Array.newInstance(k, list.size()));
    }

}

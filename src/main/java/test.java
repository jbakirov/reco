import dbmanager.DbManager;
import redis.RedisManager;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Created by Baka on 01.09.2015.
 */
public class test {

    static DbManager dbManager = new DbManager();

    public static void main(String[] args) throws UnsupportedEncodingException {

        Connection connection = null;
        try {
            connection = dbManager.getConnectionPool().getConnection();

            Statement statement = connection.createStatement();

            ResultSet rs = statement.executeQuery("SELECT * FROM books LIMIT 10");
            while (rs.next()){
                System.out.println(rs.getString(1));
                System.out.println(rs.getString(2));
                System.out.println(rs.getString(3));
                System.out.println(rs.getString(4));
                System.out.println(rs.getString(5));
                System.out.println(rs.getString(6));
                System.out.println(rs.getString(7));
            }


        } catch (SQLException e) {
            e.printStackTrace();
        }finally {
            if (connection != null){
                try {
                    connection.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }


//        byte[] salt = SecurePassword.getNextSalt();
//        String password = "Baka7182263";
//        byte[] hashed = SecurePassword.hash(password.toCharArray(), salt);
//
//        String hashedPass = Base64.encode(hashed);
//        String saltS = Base64.encode(salt);
//
//        if (SecurePassword.isExpectedPassword(password.toCharArray(), Base64.decode(saltS), Base64.decode(hashedPass))){
//            System.out.println("password matches " + hashedPass);
//        }else{
//            System.out.println("wrong password");
//        }
//        Jedis conn = new Jedis("localhost");
//        if (RedisManager.checkToken(conn, "as") == null) {
//            System.out.println("NSDNALSKLD");
//        }
//        System.out.println(RedisManager.checkToken(conn, "as"));

//        Jedis jedis = new Jedis("localhost");
//        System.out.println("Server is running: " + jedis.ping());
//
//        RedisManager.createComment(jedis, 4, "maxmustang", String.valueOf(5), "Заебал");
//
//        Set<String> commentIds = null;
//
//        commentIds = jedis.zrange("postc:id:" + 5, 0, -1);
//
//        Transaction trans = jedis.multi();
//        for (
//                String id
//                : commentIds)
//
//        {
//            trans.hgetAll("comment:" + id);
//        }
//
//        List<Map<String, String>> comments = new ArrayList<>();
//        for (
//                Object result
//                : trans.exec())
//
//        {
//            Map<String, String> comment = (Map<String, String>) result;
//            if (comment != null && comment.size() > 0) {
//                System.out.println(comment);
//            }
//        }


//
//        jedis.set("test1", new String("Серёжа".getBytes(Charset.forName("windows-1251")), "UTF-8"));
//        jedis.set("test2", new String("Гуров".getBytes(Charset.forName("UTF-8")), "UTF-8"));
//        jedis.set("test3", new String("Item".getBytes(Charset.forName("windows-1251")), "UTF-8"));
//
//        System.out.println(jedis.get("test1"));
//        System.out.println(jedis.get("test2"));
//        System.out.println(jedis.get("test3"));
    }
}

package utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * Created by Baka on 11.08.2015.
 */
public class JsonHandler {
    public static String toJson(Object o) {
        ObjectMapper objectMapper = new ObjectMapper();
        String json = "";
        try {
            json = objectMapper.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return json;
    }

    public static Object toJava(String s, String className) {
        ObjectMapper objectMapper = new ObjectMapper();
        Object object = null;
        try {
            Class<?> c = Class.forName(className);
            Constructor<?> cons = c.getConstructor();
            object = cons.newInstance();

            object = objectMapper.readValue(s, object.getClass());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        return object;
    }
}

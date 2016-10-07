package com.mservice.momo.vertx.redis;

import com.mservice.momo.util.StringConstUtil;
import com.mservice.momo.vertx.AppConstant;
import com.mservice.momo.vertx.processor.Common;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.ScanParams;
import redis.clients.jedis.ScanResult;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Created by KhoaNguyen on 9/22/2016.
 */
public class NotificationRedisVerticle extends Verticle {

    public static final String INSERT_NUMBER = "insert_number";
    public static final String REMOVE_NUMBER = "remove_number";
    public static final String GET_ALL_NOTI = "get_all_noti";
    public static final String GET_NOTI_VIA_NUMBER = "get_noti_via_number";
    public static final String INSERT_NOTI = "insert_noti";
    JsonObject glbConfig;
    Logger logger;
    private List<String> listNumber = null;
    private Jedis jedis;
    private JedisPoolConfig jedisPoolConfig;
    @Override
    public void start() {
        glbConfig = container.config();
        logger = container.logger();

        JsonObject joRedis = glbConfig.getObject("redis", new JsonObject());
        jedis = new Jedis(joRedis.getString("host", "1.1.1.1"), joRedis.getInteger("port", 6379), 120000);//http://172.16.43.14:3456/redis/core
        int numberDb = joRedis.getInteger("number_db", 1);
        jedis.select(numberDb);
        jedis.connect();

        listNumber = new ArrayList<>();
        vertx.eventBus().registerLocalHandler(AppConstant.RedisVerticle_NOTIFICATION_FROM_TOOL, new Handler<Message>() {
            @Override
            public void handle(Message msg) {
                Common.BuildLog log = new Common.BuildLog(logger);
                log.setPhoneNumber("NotificationRedisVerticle");
                JsonObject joRequest = (JsonObject)msg.body();
                String cmd = joRequest.getString(StringConstUtil.BroadcastField.COMMAND, "");
                int number = joRequest.getInteger(StringConstUtil.NUMBER, 0);
                JsonObject joData = joRequest.getObject(StringConstUtil.NOTIFICATION_OBJ, new JsonObject());
                JsonObject joReply = new JsonObject();
                log.add("cmd", cmd);
//                log.add("data RedisVerticle_NOTIFICATION_FROM_TOOL", joData.toString());
                log.add("size number RedisVerticle_NOTIFICATION_FROM_TOOL", listNumber.size());
                switch (cmd)
                {
                    case INSERT_NUMBER:
                        if (!listNumber.contains(number + ""))
                        listNumber.add(number + "");
                        break;
                    case REMOVE_NUMBER:
                        listNumber.remove(number + "");
                        break;
                    case INSERT_NOTI:
                        insertNoti(number, joData);
                        break;
                    case GET_NOTI_VIA_NUMBER:
                        joReply = getListNumberFromRedis(jedis, listNumber);
                        break;
                    case GET_ALL_NOTI:
                        joReply = getAllNotiFromRedis(jedis);
                        break;
                    default:
                        break;
                }
                msg.reply(joReply);
                log.writeLog();
            }
        });

    }

    private JsonObject getListNumberFromRedis(Jedis jedis, List<String> currentListNumber) {
        String stringNoti;
        JsonObject joObjNoti;
        JsonArray jarrObjNoti = new JsonArray();

        for (int i = 0; i < currentListNumber.size(); i++) {
            joObjNoti = new JsonObject();
            stringNoti = jedis.lpop(currentListNumber.get(i));
            if (stringNoti != null) {
                joObjNoti.putString(StringConstUtil.NUMBER, currentListNumber.get(i));
                joObjNoti.putObject(StringConstUtil.NOTIFICATION_OBJ, new JsonObject(stringNoti));
                jarrObjNoti.add(joObjNoti);
            }
        }
        JsonObject joReplyData = new JsonObject().putArray(StringConstUtil.DATA, jarrObjNoti);
        return joReplyData;
    }

    private JsonObject getAllNotiFromRedis(Jedis localJedis) {
        String stringKey;
        String stringNoti;
        JsonObject joObjNoti;
        JsonArray jarrObjNoti = new JsonArray();
        try {
            Set<String> keys = jedis.keys("*");
            if (keys != null && keys.iterator() != null) {
                Iterator<String> keyIter = keys.iterator();
                while (keyIter.hasNext()) {
                    joObjNoti = new JsonObject();
                    stringKey = keyIter.next();
                    if (stringKey != null) {
                        stringNoti = localJedis.lpop(stringKey).trim();
                        joObjNoti.putString(StringConstUtil.NUMBER, stringKey);
                        joObjNoti.putObject(StringConstUtil.NOTIFICATION_OBJ, new JsonObject(stringNoti));
                        jarrObjNoti.add(joObjNoti);
                    }
                }
            }
        } catch (Exception ex) {
            ScanParams scanParams = new ScanParams();
            scanParams.count(1000);
            ScanResult<String> scanResult = jedis.scan("0", scanParams);
            List<String> listKey = scanResult.getResult();
            for (String number : listKey) {
                joObjNoti = new JsonObject();
                stringKey = number;
                if (stringKey != null) {
                    stringNoti = localJedis.lpop(stringKey).trim();
                    joObjNoti.putString(StringConstUtil.NUMBER, stringKey);
                    joObjNoti.putObject(StringConstUtil.NOTIFICATION_OBJ, new JsonObject(stringNoti));
                    jarrObjNoti.add(joObjNoti);
                }
            }
        }

        JsonObject joReplyData = new JsonObject().putArray(StringConstUtil.DATA, jarrObjNoti);
        return joReplyData;
    }

    private void insertNoti(int number, JsonObject joNoti) {
        if (number > 0) {
            jedis.lpush(number + "", joNoti.toString());
        }
    }
}

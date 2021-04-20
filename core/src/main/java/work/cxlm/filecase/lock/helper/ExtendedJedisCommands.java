package work.cxlm.filecase.lock.helper;

import redis.clients.jedis.BinaryJedisCommands;
import redis.clients.jedis.JedisCommands;

/**
 * 针对 API 的整合（二进制数组、命令方式）
 * create 2021/4/11 18:46
 *
 * @author Chiru
 */
public interface ExtendedJedisCommands extends JedisCommands, BinaryJedisCommands {
}

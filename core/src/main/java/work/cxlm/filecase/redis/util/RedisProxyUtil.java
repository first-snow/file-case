package work.cxlm.filecase.redis.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.ShardedJedis;
import redis.clients.jedis.ShardedJedisPool;
import work.cxlm.filecase.lock.helper.ExtendedJedisCommands;
import work.cxlm.filecase.util.DefaultValueHelper;
import work.cxlm.filecase.util.SpringContextUtils;

import java.lang.reflect.Proxy;
import java.util.*;

/**
 * create 2021/4/11 18:08
 *
 * @author Chiru
 */
public class RedisProxyUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(RedisProxyUtil.class);

    /**
     * 只读函数集合
     */
    private static final Set<String> READ_ONLY_SET = new HashSet<>();

    static {
        // 初始化只读函数集合
        String[] arr = "get,exists,type,ttl,getbit,getrange,substr,hget,hmget,hexists,hlen,hkeys,hvals,hgetAll,llen,lrange,lindex,smembers,scard,sismember,srandmember,strlen,zrange,zrank,zrevrank,zrangeWithScores,zrevrangeWithScores,zcard,zscore,sort,zcount,zrangeByScore,zrevrangeByScore,zrangeByScoreWithScores,zrevrangeByScoreWithScores,zlexcount,zrangeByLex,zrevrangeByLex,echo,bitcount,hscan,sscan,zscan,pfcount".split(",");
        Collections.addAll(READ_ONLY_SET, arr);
    }

    private final String masterPoolId;
    private final String slavePoolId;

    private RedisProxyUtil(String masterPoolId, String slavePoolId) {
        this.masterPoolId = masterPoolId;
        this.slavePoolId = slavePoolId;
    }

    private final static RedisProxyUtil CACHED_INSTANCE = new RedisProxyUtil("masterJedisPool", "slaveJedisPool");

    /**
     * 获取缓存的实例以使用实例方法
     *
     * @return 缓存的本类实例
     */
    public static RedisProxyUtil getCacheInstance() {
        return CACHED_INSTANCE;
    }

    /**
     * 获得 master 连接池
     *
     * @return 如果成功则返回对应的连接池
     */
    private ShardedJedisPool getMasterPool() {
        try {
            return SpringContextUtils.getBean(masterPoolId, ShardedJedisPool.class);
        } catch (Exception e) {
            LOGGER.info(e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 获得 slave 连接池
     *
     * @return 如果成功则返回对应的连接池
     */
    private ShardedJedisPool getSlavePool() {
        try {
            return SpringContextUtils.getBean(slavePoolId, ShardedJedisPool.class);
        } catch (Exception e) {
            LOGGER.info(e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 返回 ShardedJedis 对象，注意调用后需要自行释放连接。
     *
     * @return 只使用一个连接的 ShardedJedis 对象
     */
    public ShardedJedis getMasterJedis() {
        return getMasterPool().getResource();
    }

    /**
     * 返回 ShardedJedis 对象，注意调用后释放连接。
     *
     * @return 只使用一个连接的 ShardedJedis 对象
     */
    public ShardedJedis getSlaveJedis() {
        return getSlavePool().getResource();
    }

    public static void returnJedis(ShardedJedis jedis) {
        if (jedis != null) {
            jedis.close();
        }
    }

    /**
     * 返回的 ExtendedJedisCommands 对象，支持读写分离，每次执行方法后都会自动释放连接
     *
     * @return Jedis 连接代理
     */
    public ExtendedJedisCommands getJedisReturn() {
        Object proxy = Proxy.newProxyInstance(ShardedJedisPool.class.getClassLoader(), new Class[]{ExtendedJedisCommands.class},
                (unused, method, args) -> {
                    // 读写分离，只读方法读取 slave
                    String methodName = method.getName();
                    ShardedJedisPool pool;
                    if (READ_ONLY_SET.contains(methodName)) {
                        pool = getSlavePool();
                    } else {
                        pool = getMasterPool();
                    }
                    try (ShardedJedis jedis = pool.getResource()) {
                        return method.invoke(jedis, args);
                    } catch (Exception e) {
                        LOGGER.error(e.getMessage(), e);
                        // 把 redis 当作缓存处理，若发生异常，则需要根据返回的默认值作处理，一般穿透到数据库
                        Class<?> clazz = method.getReturnType();
                        return DefaultValueHelper.getClassDefaultValue(clazz);
                    }
                });
        return (ExtendedJedisCommands) proxy;
    }

    /**
     * 返回的 ExtendedJedisCommands 对象，仅针对 master
     *
     * @return Jedis 连接代理
     */

    public ExtendedJedisCommands getMasterReturn() {
        Object proxy = Proxy.newProxyInstance(ShardedJedisPool.class.getClassLoader(), new Class[]{ExtendedJedisCommands.class},
                (proxy1, method, args) -> {
                    ShardedJedisPool pool = getMasterPool();
                    try (ShardedJedis jedis = pool.getResource()) {
                        return method.invoke(jedis, args);
                    } catch (Exception e) {
                        LOGGER.error(e.getMessage(), e);
                        // 把redis当作缓存处理，若发生异常，catch住，使用者需要针对默认返回值作处理，一般穿透到数据库
                        Class<?> clazz = method.getReturnType();
                        return DefaultValueHelper.getClassDefaultValue(clazz);
                    }
                });
        return (ExtendedJedisCommands) proxy;
    }
}


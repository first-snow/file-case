package work.cxlm.filecase.lock;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import work.cxlm.filecase.lock.helper.AbstractLock;
import work.cxlm.filecase.lock.helper.LockType;
import work.cxlm.filecase.lock.helper.RejectionPolicy;
import work.cxlm.filecase.redis.util.RedisProxyUtil;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;

/**
 * redis 实现的分布式锁
 *
 * @author Chiru
 */
@Slf4j
@Getter
public class RedisLock implements AbstractLock {

    private static final long DEFAULT_EXPIRE = 10L;
    private static final String DEFAULT_VALUE = "1";
    private static final long DEFAULT_WAIT_TIME = 3L;
    private static final String DEFAULT_MSG = null;
    private static final String NX = "NX";
    private static final String EX = "EX";
    private static final LockType DEFAULT_LOCK_TYPE = LockType.AUTO;
    private static final RejectionPolicy DEFAULT_POLICY = RejectionPolicy.ABORT;
    private static final String LOCK_RESULT = "OK";
    private static final String DEFAULT_PREFIX = "";
    private static final long DEFAULT_SLEEP = 100L;

    private final String name;
    private final long expire;
    private final String msg;
    private final RejectionPolicy policy;
    private final LockType type;
    private final String valuePrefix;

    public RedisLock(String name) {
        this(name, DEFAULT_EXPIRE);
    }

    public RedisLock(String name, long expire) {
        this(name, DEFAULT_PREFIX, expire, DEFAULT_MSG, DEFAULT_POLICY, DEFAULT_LOCK_TYPE);
    }

    public RedisLock(String name, long expire, RejectionPolicy policy) {
        this(name, DEFAULT_PREFIX, expire, DEFAULT_MSG, policy, DEFAULT_LOCK_TYPE);
    }


    public RedisLock(String name, String valuePrefix, long expire, String msg, RejectionPolicy policy, LockType type) {
        this.name = name;
        this.expire = expire;
        this.msg = msg;
        this.policy = policy;
        this.type = type;
        this.valuePrefix = StringUtils.isEmpty(valuePrefix) ? DEFAULT_PREFIX : valuePrefix;
    }

    /**
     * 阻塞性的获取锁，响应中断
     *
     * @throws InterruptedException 中断异常
     */
    @Override
    public void lockInterruptibly() throws InterruptedException {
        waitLock(DEFAULT_WAIT_TIME, TimeUnit.SECONDS);
    }

    @Override
    @SuppressWarnings("all")
    public void waitLock(long waitTime, TimeUnit unit) throws InterruptedException {
        String value = convertValue();
        long timeWait = System.currentTimeMillis() + unit.toMillis(waitTime);

        RedisProxyUtil redis = RedisProxyUtil.getCacheInstance();
        while (System.currentTimeMillis() < timeWait) {
            String result = redis.getJedisReturn().set(name, value, NX, EX, expire);
            if (LOCK_RESULT.equals(result)) {
                return;
            }
            // 自旋忙等待
            Thread.sleep(RandomUtils.nextLong(0, DEFAULT_SLEEP));
            Thread.yield();
        }
    }

    /**
     * 阻塞性的获取，不响应中断
     */
    @Override
    public void lock() {
        try {
            lockInterruptibly();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 尝试获取锁，获取不到立即返回，不阻塞
     */
    @Override
    public boolean tryLock() {
        String value = convertValue();
        String result = RedisProxyUtil.getCacheInstance().getJedisReturn().set(name, value, NX, EX, expire);
        return LOCK_RESULT.equals(result);
    }

    /**
     * 尝试获取锁，给定时间内获取不到则返回
     *
     * @param waitTime 等待时间
     * @param unit     等待时间单位
     * @return {@code true} 若成功获取到锁，{@code false} 若在指定时间内未获取到锁
     */
    @Override
    @SuppressWarnings("all")
    public boolean tryLock(long waitTime, TimeUnit unit) throws InterruptedException {
        String value = convertValue();
        long timeWait = System.currentTimeMillis() + unit.toMillis(waitTime);

        RedisProxyUtil redis = RedisProxyUtil.getCacheInstance();
        while (System.currentTimeMillis() < timeWait) {
            String result = redis.getJedisReturn().set(name, value, NX, EX, expire);
            if (LOCK_RESULT.equals(result)) {
                return true;
            }
            // 自旋忙等待
            Thread.sleep(RandomUtils.nextLong(0, DEFAULT_SLEEP));
            Thread.yield();
        }
        log.warn("等待锁超时: {}ms", waitTime);
        return false;
    }

    @Override
    public boolean isLocked() {
        return RedisProxyUtil.getCacheInstance().getJedisReturn().exists(name);
    }

    /**
     * 释放锁
     */
    @Override
    public void unlock() {
        String value = convertValue();
        String lockValue = RedisProxyUtil.getCacheInstance().getJedisReturn().get(name);

        /*解锁*/
        if (value.equalsIgnoreCase(lockValue)) {
            RedisProxyUtil.getCacheInstance().getJedisReturn().del(name);
        }
    }

    /**
     * @throws UnsupportedOperationException always
     */
    @Override
    public Condition newCondition() {
        throw new UnsupportedOperationException();
    }

    /**
     * 获取 Value
     *
     * @return 保存在 Redis 对应键中的值
     */
    private String convertValue() {
        return valuePrefix + DEFAULT_VALUE;
    }
}
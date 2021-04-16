package work.cxlm.filecase.lock.helper;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

/**
 * 分布式锁 分布式锁抽象，增加常用且 jdk 中不存在的方法
 *
 * @author zhangkairui
 * @version v1.0.0
 * @date 2018-07-17
 */
public interface AbstractLock extends Lock {

    /**
     * 分布式锁 优先考虑响应中断，而不是响应锁的普通获取或重入获取。
     *
     * @param waitTime 等待时间
     * @param unit 时间单位
     * @throws InterruptedException 线程中断异常
     */
    void waitLock(long waitTime, TimeUnit unit) throws InterruptedException;

    /**
     * 是否已加锁
     *
     * @return 是否已加锁
     */
    boolean isLocked();
}

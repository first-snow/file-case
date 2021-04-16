package work.cxlm.filecase.lock.helper;

/**
 * 分布式锁的加解锁策略
 *
 * @author Chiru
 */
public enum LockType {
    /** 自动加解锁 */
    AUTO,

    /** 锁一直存活到超时时间结束 */
    HOLD,
}

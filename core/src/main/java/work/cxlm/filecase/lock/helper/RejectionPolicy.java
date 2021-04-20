package work.cxlm.filecase.lock.helper;

import org.aspectj.lang.JoinPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import work.cxlm.filecase.exception.LockException;
import work.cxlm.filecase.lock.DsLock;
import work.cxlm.filecase.util.SpringExpressionParser;

/**
 * Redisson 锁的拒绝策略
 * create 2021/4/11 17:42
 *
 * @author Chiru
 */
public enum RejectionPolicy {

    /**
     * 中止任务，直接抛出异常
     */
    ABORT() {
        @Override
        public void reject(DsLock lock, JoinPoint point) {
            log.error("加锁失败，任务终止: {}", SpringExpressionParser.parse(lock.name(), point));
            throw new LockException("获取锁失败");
        }
    },

    /**
     * 中止任务，直接抛出超时异常
     */
    TIMEOUT_ABORT() {
        @Override
        public void reject(DsLock lock, JoinPoint point) {
            log.error("加锁超时，中止任务: {}", SpringExpressionParser.parse(lock.name(), point));
            throw new LockException("获取分布式锁超时！");
        }
    },

    /**
     * 遇到重复任务，终止任务，抛出异常
     */
    REPEAT_ABORT() {
        @Override
        public void reject(DsLock lock, JoinPoint point) {
            log.error("加锁失败，中止任务: {}", SpringExpressionParser.parse(lock.name(), point));
            throw new LockException("请勿重复提交！");
        }
    },

    /**
     * 静默处理，不执行当前任务，且返回正确结果
     */
    IGNORE() {
        @Override
        public void reject(DsLock lock, JoinPoint point) {
            log.error("加锁失败：{}，已忽略任务", SpringExpressionParser.parse(lock.name(), point));
        }
    };

    private static final Logger log = LoggerFactory.getLogger(RejectionPolicy.class);

    public abstract void reject(DsLock lock, JoinPoint point);
}

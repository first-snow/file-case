package work.cxlm.filecase.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import work.cxlm.filecase.lock.DsLock;
import work.cxlm.filecase.lock.helper.LockType;
import work.cxlm.filecase.lock.helper.RejectionPolicy;

/**
 * create 2021/4/15 12:08
 *
 * @author Chiru
 */
@Service
@Slf4j
public class LockHelperService {

    /**
     * 重复加锁时(方法执行结束之前)，直接报错，方法不会被执行
     */
    @DsLock(value = "'l1' + #p1", type = LockType.AUTO, reject = RejectionPolicy.REPEAT_ABORT, block = false)
    public int lock1(String p1) {
        mutedSleep();
        log.info(Thread.currentThread().getStackTrace()[1].getMethodName() + "执行成功：" + p1);
        return 1;
    }

    /**
     * 重复加锁时(方法执行结束之前)，静默处理，打印警告日志，方法不会被执行
     */
    @DsLock(value = "'l2' + #p1", type = LockType.AUTO, reject = RejectionPolicy.IGNORE, block = false)
    public int lock2(String p1) {
        mutedSleep();
        log.info(Thread.currentThread().getStackTrace()[1].getMethodName() + "执行成功：" + p1);
        return 1;
    }

    /**
     * 重复加锁时（2s内），直接报错，方法不会被执行
     */
    @DsLock(value = "'l3' + #p1", type = LockType.HOLD, reject = RejectionPolicy.REPEAT_ABORT, expire = 2, block = false)
    public int lock3(String p1) {
        log.info(Thread.currentThread().getStackTrace()[1].getMethodName() + "执行成功：" + p1);
        return 1;
    }

    /**
     * 重复加锁时（2s内），静默处理，打印警告日志，方法不会被执行
     */
    @DsLock(value = "'l4' + #p1", type = LockType.HOLD, reject = RejectionPolicy.IGNORE, expire = 2, block = false)
    public int lock4(String p1) {
        log.info(Thread.currentThread().getStackTrace()[1].getMethodName() + " 执行成功：" + p1);
        return 1;
    }

    /**
     * 重复加锁时（1s内），静默处理，打印警告日志，方法不会被执行
     */
    @DsLock(value = "'l5' + #p1", type = LockType.AUTO, reject = RejectionPolicy.IGNORE, timeout = 500)
    public int lock5(String p1) {
        mutedSleep();
        log.info(Thread.currentThread().getStackTrace()[1].getMethodName() + "执行成功：" + p1);
        return 1;
    }

    /**
     * 重复加锁时（1s内），抛出异常，方法不会被执行
     */
    @DsLock(value = "'l6' + #p1", type = LockType.AUTO, reject = RejectionPolicy.TIMEOUT_ABORT, timeout = 500)
    public int lock6(String p1) {
        mutedSleep();
        log.info(Thread.currentThread().getStackTrace()[1].getMethodName() + "执行成功：" + p1);
        return 1;
    }

    /**
     * 重复加锁时（1s内），抛出异常，方法不会被执行
     */
    @DsLock(value = "'l7' + #p1", type = LockType.AUTO, reject = RejectionPolicy.TIMEOUT_ABORT)
    public int lock7(String p1) {
        mutedSleep();
        log.info(Thread.currentThread().getStackTrace()[1].getMethodName() + "执行成功：" + p1);
        return 1;
    }

    private void mutedSleep() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

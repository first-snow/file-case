package work.cxlm.filecase;

import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import work.cxlm.filecase.exception.LockException;
import work.cxlm.filecase.service.LockHelperService;

import javax.annotation.Resource;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * create 2021/4/15 13:15
 *
 * @author Chiru
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = FileCaseApplication.class)
@Slf4j
public class RedisLockTest {

    @Resource
    private LockHelperService lockHelper;

    private ThreadPoolExecutor executor;

    @Before
    public void buildPoolExecutor() {
        AtomicInteger num = new AtomicInteger();
        executor = new ThreadPoolExecutor(2, 5, 3, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(), r -> new Thread(r, "Tester: " + num.incrementAndGet()));
        log.info("已重置线程池");
    }

    @After
    public void releasePool() {
        executor.shutdown();
        log.info("已关闭线程池");
    }

    @Test
    public void autoRepeatAbortNonBlockLockTest() {
        String lockName = "自动解锁、报错、非阻塞、不等待";
        AtomicInteger counter = new AtomicInteger(0);
        Runnable task = () -> {
            try {
                counter.addAndGet(lockHelper.lock1(lockName));
            } catch (LockException lockException) {
                log.warn("加锁失败：{}", lockException.getMessage());
            }
        };
        executor.submit(task);
        executor.submit(task);
        // 等待执行完毕
        mutedSleep(2000);
        Assert.assertEquals(1, counter.get());
        log.info("========== 用例分割线 =========");
        // 自动解锁的情况，顺序加锁时都成功
        int serialLock = lockHelper.lock1(lockName);
        serialLock += lockHelper.lock1(lockName);
        mutedSleep(2500);
        Assert.assertEquals(2, serialLock);
    }

    @Test
    public void autoRepeatAbortNonBlockMutedLockTest() {
        String lockName = "自动解锁、静默、非阻塞、不等待";
        AtomicInteger counter = new AtomicInteger(0);
        executor.submit(() -> counter.addAndGet(lockHelper.lock2(lockName)));
        mutedSleep(1500);
        Assert.assertEquals(1, counter.get());
        // 自动解锁的情况，顺序加锁时都成功
        int serialLock = lockHelper.lock2(lockName);
        serialLock += lockHelper.lock2(lockName);
        mutedSleep(2500);
        Assert.assertEquals(2, serialLock);
    }

    @Test
    public void holdRepeatAbortNonBlockLockTest1() {
        // 确保使用通一把锁进行测试的锁已过期
        mutedSleep(2000);
        String lockName = "不自动解锁、报错、非阻塞、不等待";
        // 不自动解锁的情况，顺序加锁时都成功
        lockHelper.lock3(lockName);
        try {
            lockHelper.lock3(lockName);
            Assert.fail();
        } catch (LockException e) {
            log.warn("测试通过，加锁失败：{}", e.getMessage());
        }
    }

    @Test
    public void holdRepeatAbortNonBlockLockTest2() {
        // 确保使用通一把锁进行测试的锁已过期
        mutedSleep(2000);
        String lockName = "不自动解锁、报错、非阻塞、不等待";
        int serialLock = lockHelper.lock3(lockName);
        // 等待解锁后，两次加锁都应该成功
        mutedSleep(3000);
        serialLock += lockHelper.lock3(lockName);
        Assert.assertEquals(2, serialLock);
    }

    @Test
    public void holdRepeatMutedNonBlockLockTest() {
        // 确保使用同一把锁进行测试的锁已过期
        mutedSleep(2000);
        String lockName = "不自动解锁、静默、非阻塞、不等待";
        int serialLock = lockHelper.lock4(lockName);
        serialLock += lockHelper.lock4(lockName);
        Assert.assertEquals(1, serialLock);

        mutedSleep(1500L);
        AtomicInteger counter = new AtomicInteger();
        executor.submit(()->counter.addAndGet(lockHelper.lock4(lockName)));
        executor.submit(()->counter.addAndGet(lockHelper.lock4(lockName)));
        mutedSleep(500L);
        Assert.assertEquals(1, serialLock);
    }

    @Test
    public void autoMutedBlockLockTest() {
        String lockName = "自动解锁、静默、阻塞锁";
        AtomicInteger counter = new AtomicInteger(0);
        executor.submit(() -> counter.addAndGet(lockHelper.lock5(lockName)));
        mutedSleep(1500);
        Assert.assertEquals(1, counter.get());

        executor.submit(() -> counter.addAndGet(lockHelper.lock5(lockName)));
        executor.submit(() -> counter.addAndGet(lockHelper.lock5(lockName)));
        mutedSleep(2500);
        Assert.assertEquals(2, counter.get());
    }

    @Test
    public void autoBlockLockTest1() {
        String lockName = "自动解锁、报错、阻塞锁";
        AtomicInteger counter = new AtomicInteger();
        Runnable task = () -> {
            try {
                counter.addAndGet(lockHelper.lock6(lockName));
            } catch (LockException lockException) {
                log.warn("加锁失败：{}", lockException.getMessage());
            }
        };
        executor.submit(task);
        executor.submit(task);
        mutedSleep(1500);
        Assert.assertEquals(1, counter.get());
    }

    @Test
    public void autoBlockLockTest2() {
        String lockName = "自动解锁、报错、阻塞锁";

        int serialLock = lockHelper.lock7(lockName);
        serialLock += lockHelper.lock7(lockName);
        serialLock += lockHelper.lock7(lockName);
        Assert.assertEquals(3, serialLock);
    }

    @Test
    public void passOnExceptionTest() {
        mutedSleep(2000);
        AtomicInteger counter = new AtomicInteger(0);
        Runnable task = () -> counter.addAndGet(lockHelper.lock3("报错后跳过原方法"));
        // 推入 5 个线程，只有一个加锁、执行成功
        executor.submit(task);
        executor.submit(task);
        executor.submit(task);
        executor.submit(task);
        executor.submit(task);
        mutedSleep(5000);
        Assert.assertEquals(1, counter.get());
    }

    @Test
    public void passOnIgnoreTest() {
        mutedSleep(2000);
        AtomicInteger counter = new AtomicInteger(0);
        Runnable task = () -> counter.addAndGet(lockHelper.lock4("忽略时跳过原方法"));
        // 推入 5 个线程，只有一个加锁、执行成功
        executor.submit(task);
        executor.submit(task);
        executor.submit(task);
        executor.submit(task);
        executor.submit(task);
        mutedSleep(5000);
        Assert.assertEquals(1, counter.get());
    }

    private void mutedSleep(long time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            log.error("被叫醒", e);
        }
        log.info("睡了 {} ms", time);
    }
}

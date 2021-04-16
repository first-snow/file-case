package work.cxlm.filecase.lock.helper;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import work.cxlm.filecase.lock.DsLock;
import work.cxlm.filecase.lock.RedisLock;
import work.cxlm.filecase.util.DefaultValueHelper;
import work.cxlm.filecase.util.SpringExpressionParser;

import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * create 2021/4/13 17:47
 *
 * @author Chiru
 */
@Slf4j
@Aspect
@Order(1)
@Component
public class DsLockAspect {

    private static final String POINT = ".";

    private static final ThreadLocal<RedisLock> LOCK_THREAD_LOCAL = new ThreadLocal<>();

    @Pointcut(value = "@annotation(work.cxlm.filecase.lock.DsLock)")
    public void pointCut() {
    }

    /**
     * 设置前置通知，不能有返回值
     */
    @Around(value = "pointCut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        boolean lockSuccess = preHandler(joinPoint);
        if (lockSuccess) {
            return joinPoint.proceed();
        }
        // 跳过执行时，返回值给它一个默认值（否则都返回 null 会在基本类型上报错）
        // 不可以直接抛异常，在 IGNORE 策略下抛异常是不行的
        Class<?> returnType = ((MethodSignature) joinPoint.getSignature()).getMethod().getReturnType();
        return DefaultValueHelper.getClassDefaultValue(returnType);
    }

    /**
     * 前置处理
     *
     * @return 是否需要执行该方法
     */
    public boolean preHandler(ProceedingJoinPoint point) throws UnknownHostException, InterruptedException {
        /* 构造 Redis 锁对象 */
        Method targetMethod = ((MethodSignature) point.getSignature()).getMethod();
        DsLock dsLockAnnotation = AnnotatedElementUtils.getMergedAnnotation(targetMethod, DsLock.class);
        if (dsLockAnnotation == null) {
            throw new NullPointerException("注解失效，method: " + targetMethod);
        }
        String key = generateLockKey(point, dsLockAnnotation);

        /* 获取主机名 + 线程名 */
        InetAddress inetAddress = InetAddress.getLocalHost();
        String valuePrefix = inetAddress.getHostName() + "_" + inetAddress.getHostAddress() + "_" +
                Thread.currentThread().getId() + "_" + Thread.currentThread().getName() + "_";

        /* 尝试加锁 */
        log.info("尝试加锁: {}, 主机、线程名: {}", key, valuePrefix);
        RedisLock lock = new RedisLock(key, valuePrefix, dsLockAnnotation.expire(),
                dsLockAnnotation.msg(), dsLockAnnotation.reject(), dsLockAnnotation.type());
        boolean blockLock = dsLockAnnotation.block();
        boolean lockSuccess = (!blockLock && lock.tryLock()) ||
                (blockLock && lock.tryLock(dsLockAnnotation.timeout(), TimeUnit.MILLISECONDS));
        if (lockSuccess) {
            /* 加锁成功，把 key 放入 ThreadLocal */
            log.info("加锁成功: {}, 主机线程名: {}", key, valuePrefix);
            LOCK_THREAD_LOCAL.set(lock);
        } else {
            /* 加锁失败，错误处理 */
            lock.getPolicy().reject(dsLockAnnotation, point);
        }
        return lockSuccess;
    }

    /**
     * 加锁成功且代理方法执行成功，分情况处理
     */
    @AfterReturning(value = "pointCut()")
    private void afterReturning() {
        try {
            RedisLock lock = LOCK_THREAD_LOCAL.get();
            // 自动加解锁（且锁有效）时，在方法执行结束后释放锁
            if (null != lock && LockType.AUTO == lock.getType()) {
                log.info("释放锁: {}", lock.getName());
                lock.unlock();
            }
        } finally {
            LOCK_THREAD_LOCAL.remove();
        }
    }

    /**
     * 加锁失败或代理方法执行失败
     */
    @AfterThrowing(value = "pointCut()")
    private void afterThrowing() {
         /* 即使 before() 中抛出异常，也会执行 after() && afterThrowing()
         所以判断 ThreadLocal 是否为空，避免其它线程释放掉之前线程的锁
         加锁失败不处理，代理方法失败时释放锁并清空 ThreadLocal */
        if (LOCK_THREAD_LOCAL.get() != null) {
            try {
                RedisLock lock = LOCK_THREAD_LOCAL.get();
                log.info("释放锁: {}", lock.getName());
                lock.unlock();
            } finally {
                LOCK_THREAD_LOCAL.remove();
            }
        }
    }

    /**
     * 生成 lock.className.methodName 签名
     */
    private String generateClassMethodName(JoinPoint point) {
        final String prefixKey = "lock";
        String className = point.getSignature().getDeclaringType().getSimpleName();
        String methodName = point.getSignature().getName();
        return prefixKey + POINT + className + POINT + methodName;
    }

    /**
     * 生成锁需要使用的 key
     */
    private String generateLockKey(JoinPoint point, DsLock annotation) {
        String prefix = generateClassMethodName(point) + POINT;
        if (!annotation.name().isEmpty()) {
            return prefix + SpringExpressionParser.parse(annotation.name(), point);
        }
        return prefix + UUID.randomUUID();
    }
}

package work.cxlm.filecase.lock;

import org.springframework.core.annotation.AliasFor;
import work.cxlm.filecase.lock.helper.LockType;
import work.cxlm.filecase.lock.helper.RejectionPolicy;

import java.lang.annotation.*;

/**
 * 分布式锁
 * create 2021/4/11 17:40
 *
 * @author Chiru
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Inherited
public @interface DsLock {
    /**
     * 自定义锁对象的 key 值，默认 uuid
     */
    @AliasFor("name")
    String value() default "";

    @AliasFor("value")
    String name() default "";

    /**
     * 线程阻塞时的处理方法，默认中止任务，抛出异常
     */
    RejectionPolicy reject() default RejectionPolicy.ABORT;

    /**
     * 对象加锁的方式，默认自动
     */
    LockType type() default LockType.AUTO;

    /**
     * 抛出异常时的提示信息
     */
    String msg() default "请勿重复提交";

    /**
     * 分布式锁的过期时间，单位秒，默认 10s
     */
    long expire() default 10;

    /**
     * 等待锁期间是否阻塞
     * false：则获取不到锁直接返回失败，不会等待
     * true：获取锁期间会忙等待，直到超时或者得到锁
     */
    boolean block() default true;

    /**
     * block 为 true 时有效，等待锁的最大时长，单位 ms，默认 2s
     */
    long timeout() default 2000;
}

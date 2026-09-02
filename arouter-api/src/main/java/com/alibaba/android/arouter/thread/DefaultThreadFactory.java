package com.alibaba.android.arouter.thread;

import androidx.annotation.NonNull;

import com.alibaba.android.arouter.launcher.ARouter;
import com.alibaba.android.arouter.utils.Consts;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 线程池工厂类
 *
 * @author zhilong <a href="mailto:zhilong.liu@aliyun.com">Contact me.</a>
 * @version 1.0
 * @since 15/12/25 上午10:51
 */
public class DefaultThreadFactory implements ThreadFactory {
    private static final AtomicInteger poolNumber = new AtomicInteger(1);

    private final AtomicInteger threadNumber = new AtomicInteger(1);
    private final ThreadGroup group;
    private final String namePrefix;

    public DefaultThreadFactory() {
        SecurityManager s = System.getSecurityManager();
        group = (s != null) ? s.getThreadGroup() :
                Thread.currentThread().getThreadGroup();
        namePrefix = "ARouter task pool No." + poolNumber.getAndIncrement() + ", thread No.";
    }

    public Thread newThread(@NonNull Runnable runnable) {
        String threadName = namePrefix + threadNumber.getAndIncrement();
        ARouter.logger.info(Consts.TAG, "Thread production, name is [" + threadName + "]");
        Thread thread = new Thread(group, runnable, threadName, 0);
        if (thread.isDaemon()) {   //设为非后台线程
            thread.setDaemon(false);
        }
        if (thread.getPriority() != Thread.NORM_PRIORITY) { //优先级为normal
            thread.setPriority(Thread.NORM_PRIORITY);
        }

        // 捕获多线程处理中的异常
        thread.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable ex) {
                ARouter.logger.info(Consts.TAG, "Running task appeared exception! Thread [" + thread.getName() + "], because [" + ex.getMessage() + "]");
            }
        });
        return thread;
    }
}
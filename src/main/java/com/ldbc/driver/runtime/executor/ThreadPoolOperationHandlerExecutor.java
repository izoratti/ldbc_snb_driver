package com.ldbc.driver.runtime.executor;

import com.ldbc.driver.OperationHandler;
import com.ldbc.driver.temporal.Duration;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class ThreadPoolOperationHandlerExecutor implements OperationHandlerExecutor {
    private final ExecutorService threadPoolExecutorService;
    private final AtomicLong uncompletedHandlers = new AtomicLong(0);
    private final AtomicBoolean shutdown = new AtomicBoolean(false);

    public ThreadPoolOperationHandlerExecutor(int threadCount) {
        ThreadFactory threadFactory = new ThreadFactory() {
            private final long factoryTimeStampId = System.currentTimeMillis();
            int count = 0;

            @Override
            public Thread newThread(Runnable runnable) {
                Thread newThread = new Thread(
                        runnable,
                        ThreadPoolOperationHandlerExecutor.class.getSimpleName() + "-id(" + factoryTimeStampId + ")" + "-thread(" + count++ + ")");
                return newThread;
            }
        };
        this.threadPoolExecutorService = ThreadPoolExecutorWithAfterExecute.newFixedThreadPool(threadCount, threadFactory, uncompletedHandlers);
    }

    @Override
    synchronized public final void execute(OperationHandler<?> operationHandler) {
        uncompletedHandlers.incrementAndGet();
        threadPoolExecutorService.execute(operationHandler);
    }

    @Override
    synchronized public final void shutdown(Duration wait) throws OperationHandlerExecutorException {
        if (shutdown.get())
            throw new OperationHandlerExecutorException("Executor has already been shutdown");
        try {
            threadPoolExecutorService.shutdown();
            boolean allHandlersCompleted = threadPoolExecutorService.awaitTermination(wait.asMilli(), TimeUnit.MILLISECONDS);
            if (false == allHandlersCompleted) {
                List<Runnable> stillRunningThreads = threadPoolExecutorService.shutdownNow();
                if (false == stillRunningThreads.isEmpty()) {
                    throw new OperationHandlerExecutorException(String.format("Executor shutdown before all handlers could complete - %s are still running", uncompletedHandlers));
                }
            }
        } catch (Exception e) {
            throw new OperationHandlerExecutorException("Error encountered while trying to shutdown", e);
        }
        shutdown.set(true);
    }

    @Override
    public long uncompletedOperationHandlerCount() throws OperationHandlerExecutorException {
        return uncompletedHandlers.get();
    }

    private static class ThreadPoolExecutorWithAfterExecute extends ThreadPoolExecutor {
        public static ThreadPoolExecutorWithAfterExecute newFixedThreadPool(int threadCount, ThreadFactory threadFactory, AtomicLong uncompletedHandlers) {
            int corePoolSize = threadCount;
            int maximumPoolSize = threadCount;
            long keepAliveTime = 0;
            TimeUnit unit = TimeUnit.MILLISECONDS;
            BlockingQueue<Runnable> workQueue = new LinkedTransferQueue<>();
            return new ThreadPoolExecutorWithAfterExecute(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, uncompletedHandlers);
        }

        private final AtomicLong uncompletedHandlers;

        private ThreadPoolExecutorWithAfterExecute(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory, AtomicLong uncompletedHandlers) {
            super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
            this.uncompletedHandlers = uncompletedHandlers;
        }

        @Override
        protected void afterExecute(Runnable operationHandler, Throwable throwable) {
            super.afterExecute(operationHandler, throwable);
            ((OperationHandler) operationHandler).onComplete();
            ((OperationHandler) operationHandler).cleanup();
            uncompletedHandlers.decrementAndGet();
        }
    }
}

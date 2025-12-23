package common.extensions;

import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class TimingExtension implements BeforeTestExecutionCallback, AfterTestExecutionCallback {
    private final ThreadLocal<Long> startTime = new ThreadLocal<>();

    @Override
    public void beforeTestExecution(ExtensionContext context) throws Exception {
        startTime.set(System.currentTimeMillis());
        String testName = context.getRequiredTestClass().getPackageName() + "." + context.getDisplayName();
        System.out.println("Thread " + Thread.currentThread().getName() + ": Test started " + testName);
    }

    @Override
    public void afterTestExecution(ExtensionContext context) throws Exception {
        String testName = context.getRequiredTestClass().getPackageName() + "." + context.getDisplayName();
        Long start = startTime.get();
        if (start == null) {
            System.out.println("Thread " + Thread.currentThread().getName() + ": Test finished " + testName + ", start time not recorded.");
            return;
        }
        long duration = System.currentTimeMillis() - start;
        System.out.println("Thread " + Thread.currentThread().getName() + ": Test finished " + testName + ", test duration " + duration + " ms");
        startTime.remove(); // очищаем ThreadLocal
    }
}

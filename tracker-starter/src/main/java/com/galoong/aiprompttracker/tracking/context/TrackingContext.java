package com.galoong.aiprompttracker.tracking.context;

import com.galoong.aiprompttracker.tracking.storage.ExecutionStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Thread-local context for tracking AI executions and calls.
 *
 * This manages the execution lifecycle:
 * 1. Start execution (create context)
 * 2. Track calls (add to context)
 * 3. End execution (persist via ExecutionStore)
 * 4. Clear context
 */
@Slf4j
@Component
public class TrackingContext {

    private static final ThreadLocal<ExecutionContext> CURRENT_EXECUTION = new ThreadLocal<>();

    private static ExecutionStore executionStore;

    @Autowired
    public void setExecutionStore(ExecutionStore store) {
        TrackingContext.executionStore = store;
    }

    /**
     * Start tracking a new execution
     */
    public static void startExecution(ExecutionContext context) {
        CURRENT_EXECUTION.set(context);
        log.debug("Started tracking execution: {}", context.getExecutionId());
    }

    /**
     * Get the current execution context
     */
    public static ExecutionContext getCurrentExecution() {
        return CURRENT_EXECUTION.get();
    }

    /**
     * Check if we're currently tracking an execution
     */
    public static boolean isTracking() {
        return CURRENT_EXECUTION.get() != null;
    }

    /**
     * Add a call to the current execution
     */
    public static void addCall(CallRecordData call) {
        ExecutionContext context = CURRENT_EXECUTION.get();
        if (context != null) {
            context.addCall(call);
            log.debug("Added call to execution {}: provider={}, model={}",
                context.getExecutionId(), call.getProvider(), call.getModel());
        } else {
            log.warn("Attempted to add call but no execution context exists");
        }
    }

    /**
     * Mark the current execution as successful and persist it
     */
    public static void endExecutionSuccess() {
        ExecutionContext context = CURRENT_EXECUTION.get();
        if (context != null) {
            context.markSuccess();
            persistExecution(context);
            log.debug("Ended execution successfully: {}", context.getExecutionId());
        }
    }

    /**
     * Mark the current execution as failed and persist it
     */
    public static void endExecutionError(Throwable ex) {
        ExecutionContext context = CURRENT_EXECUTION.get();
        if (context != null) {
            context.markError(ex.getMessage());
            persistExecution(context);
            log.debug("Ended execution with error: {}", context.getExecutionId());
        }
    }

    /**
     * Clear the current execution context
     */
    public static void clear() {
        ExecutionContext context = CURRENT_EXECUTION.get();
        if (context != null) {
            log.debug("Clearing execution context: {}", context.getExecutionId());
        }
        CURRENT_EXECUTION.remove();
    }

    /**
     * Persist the execution via ExecutionStore
     */
    private static void persistExecution(ExecutionContext context) {
        try {
            if (executionStore != null) {
                executionStore.save(context);
            } else {
                log.warn("ExecutionStore not available, cannot persist execution");
            }
        } catch (Exception e) {
            log.error("Failed to persist execution: {}", context.getExecutionId(), e);
        }
    }
}

package com.galoong.aiprompttracker.tracking.storage;

import com.galoong.aiprompttracker.tracking.context.ExecutionContext;
import lombok.extern.slf4j.Slf4j;

/**
 * No-op implementation of ExecutionStore that discards all data.
 *
 * <p>Used when persistence is disabled ({@code ai-prompts.tracking.persistence.mode=none}).
 * Tracking still works (AOP, interceptors, metrics in-memory) but data is not persisted.
 */
@Slf4j
public class NoopExecutionStore implements ExecutionStore {

    @Override
    public void save(ExecutionContext context) {
        log.debug("Persistence disabled. Execution not saved: id={}, function={}, calls={}, cost={}",
                context.getExecutionId(),
                context.getFunctionName(),
                context.getCallsCount(),
                context.getTotalCost());
    }

    @Override
    public boolean isPersistent() {
        return false;
    }
}

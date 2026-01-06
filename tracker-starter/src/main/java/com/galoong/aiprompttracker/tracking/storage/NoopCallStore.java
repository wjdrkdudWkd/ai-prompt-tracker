package com.galoong.aiprompttracker.tracking.storage;

import com.galoong.aiprompttracker.tracking.collector.CallRecordInput;
import lombok.extern.slf4j.Slf4j;

/**
 * No-op implementation of CallStore that discards all data.
 *
 * <p>Used when persistence is disabled ({@code ai-prompts.tracking.persistence.mode=none}).
 * Call tracking still works (metrics aggregated in ExecutionContext) but data is not persisted.
 */
@Slf4j
public class NoopCallStore implements CallStore {

    @Override
    public void save(String executionId, CallRecordInput input) {
        log.debug("Persistence disabled. Call not saved: executionId={}, provider={}, model={}, latency={}ms",
                executionId,
                input.getProvider(),
                input.getModel(),
                input.getLatencyMs());
    }

    @Override
    public boolean isPersistent() {
        return false;
    }
}

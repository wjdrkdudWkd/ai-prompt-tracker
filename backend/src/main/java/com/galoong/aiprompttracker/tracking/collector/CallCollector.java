package com.galoong.aiprompttracker.tracking.collector;

/**
 * Interface for collecting AI API call records.
 * Implementations should handle recording calls to the current execution context.
 */
public interface CallCollector {

    /**
     * Record an AI API call
     *
     * @param input The call metadata
     */
    void recordCall(CallRecordInput input);
}

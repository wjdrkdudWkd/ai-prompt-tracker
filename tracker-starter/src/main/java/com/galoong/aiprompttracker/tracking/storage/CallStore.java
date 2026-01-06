package com.galoong.aiprompttracker.tracking.storage;

import com.galoong.aiprompttracker.tracking.collector.CallRecordInput;

/**
 * Abstraction for storing call records.
 *
 * <p>Implementations:
 * <ul>
 *   <li>{@link NoopCallStore} - discards data (default when persistence disabled)</li>
 *   <li>{@link JpaCallStore} - persists to database via JPA (when JDBC mode enabled)</li>
 * </ul>
 */
public interface CallStore {

    /**
     * Save a call record.
     *
     * @param executionId the parent execution ID
     * @param input the call record input to save
     */
    void save(String executionId, CallRecordInput input);

    /**
     * Check if this store actually persists data.
     *
     * @return true if data is persisted, false if discarded
     */
    default boolean isPersistent() {
        return true;
    }
}

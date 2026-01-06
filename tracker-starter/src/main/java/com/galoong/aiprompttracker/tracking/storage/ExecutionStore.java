package com.galoong.aiprompttracker.tracking.storage;

import com.galoong.aiprompttracker.tracking.context.ExecutionContext;

/**
 * Abstraction for storing execution records.
 *
 * <p>Implementations:
 * <ul>
 *   <li>{@link NoopExecutionStore} - discards data (default when persistence disabled)</li>
 *   <li>{@link JpaExecutionStore} - persists to database via JPA (when JDBC mode enabled)</li>
 * </ul>
 */
public interface ExecutionStore {

    /**
     * Save an execution record.
     *
     * @param context the execution context to save
     */
    void save(ExecutionContext context);

    /**
     * Check if this store actually persists data.
     *
     * @return true if data is persisted, false if discarded
     */
    default boolean isPersistent() {
        return true;
    }
}

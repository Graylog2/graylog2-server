package org.graylog.scheduler;

/**
 * Used by the scheduler to configure itself.
 */
public interface JobSchedulerConfig {
    /**
     * Determines if the scheduler is can start.
     *
     * @return true if the scheduler can be started, false otherwise
     */
    boolean canRun();

    /**
     * The number of worker threads to start.
     *
     * @return number of worker threads
     */
    int numberOfWorkerThreads();
}

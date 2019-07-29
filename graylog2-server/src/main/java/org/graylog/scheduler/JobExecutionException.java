package org.graylog.scheduler;

import static java.util.Objects.requireNonNull;

/**
 * This is thrown when a {@link Job} failed to execute correctly.
 */
public class JobExecutionException extends Exception {
    private final JobTriggerDto trigger;
    private final JobTriggerUpdate update;

    public JobExecutionException(String message, JobTriggerDto trigger, JobTriggerUpdate update) {
        this(message, trigger, update, null);
    }

    public JobExecutionException(String message, JobTriggerDto trigger, JobTriggerUpdate update, Throwable cause) {
        super(message, cause);
        this.trigger = requireNonNull(trigger, "trigger cannot be null");
        this.update = requireNonNull(update, "update cannot be null");
    }

    /**
     * Returns the trigger that triggered the job execution.
     *
     * @return the related trigger
     */
    public JobTriggerDto getTrigger() {
        return trigger;
    }

    /**
     * Returns the trigger update that should be stored in the database.
     *
     * @return the trigger update or null if not set
     */
    public JobTriggerUpdate getUpdate() {
        return update;
    }
}

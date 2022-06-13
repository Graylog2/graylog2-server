package org.graylog.scheduler.rest;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class JobTriggerDetails {
    public static JobTriggerDetails EMPTY_DETAILS = create("", "", false);

    public abstract String info();

    public abstract String description();

    public abstract boolean isCancallable();

    public static JobTriggerDetails create(String info, String description, boolean isCancallable) {
        return new AutoValue_JobTriggerDetails(info, description, isCancallable);
    }

}

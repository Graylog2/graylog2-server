package org.graylog.plugins.views.search.events;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.google.auto.value.AutoValue;
import org.graylog.plugins.views.search.SearchJob;
import org.graylog2.plugin.database.users.User;
import org.joda.time.DateTime;

@AutoValue
@JsonAutoDetect
public abstract class SearchJobExecutionEvent {
    public abstract User user();
    public abstract SearchJob searchJob();
    public abstract DateTime executionStart();

    public static SearchJobExecutionEvent create(User user, SearchJob searchJob, DateTime executionStart) {
        return new AutoValue_SearchJobExecutionEvent(user, searchJob, executionStart);
    }
}

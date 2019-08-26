package org.graylog.integrations.aws.resources.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;

import java.util.List;

@JsonAutoDetect
@AutoValue
@WithBeanGetter
public abstract class LogGroupsResponse {

    private static final String LOG_GROUPS = "log_groups";
    private static final String TOTAL = "total";

    @JsonProperty(LOG_GROUPS)
    public abstract List<String> logGroups();

    @JsonProperty(TOTAL)
    public abstract long total();

    public static LogGroupsResponse create(@JsonProperty(LOG_GROUPS) List<String> logGroups,
                                           @JsonProperty(TOTAL) long total) {
        return new AutoValue_LogGroupsResponse(logGroups, total);
    }
}
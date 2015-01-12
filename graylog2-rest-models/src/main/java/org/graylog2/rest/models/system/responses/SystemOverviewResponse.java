package org.graylog2.rest.models.system.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

@AutoValue
@JsonAutoDetect
public abstract class SystemOverviewResponse {
    @JsonProperty
    public abstract String facility();
    @JsonProperty
    public abstract String codename();
    @JsonProperty
    public abstract String serverId();
    @JsonProperty
    public abstract String version();
    @JsonProperty
    public abstract String startedAt();
    @JsonProperty("is_processing")
    public abstract boolean isProcessing();
    @JsonProperty
    public abstract String hostname();
    @JsonProperty
    public abstract String lifecycle();
    @JsonProperty
    public abstract String lbStatus();
    @JsonProperty
    public abstract String timezone();

    @JsonCreator
    public static SystemOverviewResponse create(@JsonProperty("facility") String facility,
                                                @JsonProperty("codename") String codename,
                                                @JsonProperty("server_id") String serverId,
                                                @JsonProperty("version") String version,
                                                @JsonProperty("started_at") String startedAt,
                                                @JsonProperty("is_processing") boolean isProcessing,
                                                @JsonProperty("hostname") String hostname,
                                                @JsonProperty("lifecycle") String lifecycle,
                                                @JsonProperty("lb_status") String lbStatis,
                                                @JsonProperty("timezone") String timezone) {
        return new AutoValue_SystemOverviewResponse(facility, codename, serverId, version, startedAt, isProcessing, hostname, lifecycle, lbStatis, timezone);
    }
}

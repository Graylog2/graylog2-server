package org.graylog.integrations.aws.resources.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;

import java.util.List;

@JsonAutoDetect
@AutoValue
@WithBeanGetter
public abstract class AvailableServiceResponse {

    private static final String SERVICES = "services";
    private static final String TOTAL = "total";

    @JsonProperty(SERVICES)
    public abstract List<AvailableService> services();

    @JsonProperty(TOTAL)
    public abstract long total();

    public static AvailableServiceResponse create(@JsonProperty(SERVICES) List<AvailableService> services,
                                                  @JsonProperty(TOTAL) long total) {
        return new AutoValue_AvailableServiceResponse(services, total);
    }
}
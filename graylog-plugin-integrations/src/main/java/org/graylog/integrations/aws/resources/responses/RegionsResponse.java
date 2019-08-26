package org.graylog.integrations.aws.resources.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;

import java.util.List;

@JsonAutoDetect
@AutoValue
@WithBeanGetter
public abstract class RegionsResponse {

    private static final String REGIONS = "regions";
    private static final String TOTAL = "total";

    @JsonProperty(REGIONS)
    public abstract List<AWSRegion> regions();

    @JsonProperty(TOTAL)
    public abstract long total();

    public static RegionsResponse create(@JsonProperty(REGIONS) List<AWSRegion> regions,
                                         @JsonProperty(TOTAL) long total) {
        return new AutoValue_RegionsResponse(regions, total);
    }
}
package org.graylog.integrations.aws.resources.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;

import java.util.List;

@JsonAutoDetect
@AutoValue
@WithBeanGetter
public abstract class StreamsResponse {

    private static final String STREAMS = "streams";
    private static final String TOTAL = "total";

    @JsonProperty(STREAMS)
    public abstract List<String> streams();

    @JsonProperty(TOTAL)
    public abstract long total();

    public static StreamsResponse create(@JsonProperty(STREAMS) List<String> streams,
                                         @JsonProperty(TOTAL) long total) {
        return new AutoValue_StreamsResponse(streams, total);
    }
}
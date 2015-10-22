package org.graylog2.rest.resources.streams.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

@AutoValue
@JsonAutoDetect
public abstract class StreamRuleTypeResponse {
    @JsonProperty
    public abstract int id();

    @JsonProperty
    public abstract String name();

    @JsonProperty("short_desc")
    public abstract String shortDesc();

    @JsonProperty("long_desc")
    public abstract String longDesc();

    @JsonCreator
    public static StreamRuleTypeResponse create(@JsonProperty("id") int id,
                                                @JsonProperty("name") String name,
                                                @JsonProperty("short_desc") String shortDesc,
                                                @JsonProperty("long_desc") String longDesc) {
        return new AutoValue_StreamRuleTypeResponse(id, name, shortDesc, longDesc);
    }
}

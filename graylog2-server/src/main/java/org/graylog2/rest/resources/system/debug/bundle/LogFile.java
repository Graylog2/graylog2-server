package org.graylog2.rest.resources.system.debug.bundle;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

@JsonAutoDetect
public record LogFile (@JsonProperty("id") String id,
                       @JsonProperty("name")String name,
                       @JsonProperty("size") long size,
                       @JsonProperty("lastModified") Instant lastModified) {}


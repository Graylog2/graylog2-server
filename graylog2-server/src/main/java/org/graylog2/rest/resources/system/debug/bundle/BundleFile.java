package org.graylog2.rest.resources.system.debug.bundle;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonAutoDetect
public record BundleFile(@JsonProperty("file_name") String fileName,
                         @JsonProperty("size") long size) {
}

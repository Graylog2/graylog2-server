package org.graylog2.rest.resources.streams.responses;

import com.fasterxml.jackson.annotation.JsonProperty;

public record StreamCreatedResponse(@JsonProperty("stream_id") String streamId) {
}

package org.graylog2.restclient.models.api.responses.streams;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class CreateStreamResponse {
    @JsonProperty("stream_id")
    public String streamId;
}

package org.graylog2.restclient.models.api.responses;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class BufferClassesResponse {
    @JsonProperty("process_buffer")
    public String processBufferClass;
    @JsonProperty("output_buffer")
    public String outputBufferClass;
}

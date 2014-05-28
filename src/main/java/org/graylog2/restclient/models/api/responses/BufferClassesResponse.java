package org.graylog2.restclient.models.api.responses;

import com.google.gson.annotations.SerializedName;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class BufferClassesResponse {
    @SerializedName("process_buffer")
    public String processBufferClass;
    @SerializedName("output_buffer")
    public String outputBufferClass;
}

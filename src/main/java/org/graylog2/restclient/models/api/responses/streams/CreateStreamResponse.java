package org.graylog2.restclient.models.api.responses.streams;

import com.google.gson.annotations.SerializedName;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class CreateStreamResponse {
    @SerializedName("stream_id")
    public String streamId;
}

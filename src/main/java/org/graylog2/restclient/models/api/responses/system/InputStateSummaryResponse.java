package org.graylog2.restclient.models.api.responses.system;

import com.google.gson.annotations.SerializedName;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class InputStateSummaryResponse {
    public String id;
    public String state;

    @SerializedName("message_input")
    public InputSummaryResponse messageinput;

    @SerializedName("started_at")
    public String startedAt;

    @SerializedName("detailed_message")
    public String detailedMessage;
}

package org.graylog2.restclient.models.api.responses.system;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class InputStateSummaryResponse {
    public String id;
    public String state;

    @JsonProperty("message_input")
    public InputSummaryResponse messageinput;

    @JsonProperty("started_at")
    public String startedAt;

    @JsonProperty("detailed_message")
    public String detailedMessage;
}

package org.graylog2.restclient.models.api.responses.system;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class InputLaunchResponse {
    @JsonProperty("input_id")
    public String inputId;

    @JsonProperty("persist_id")
    public String persistId;
}

package org.graylog2.restclient.models.api.responses;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class SessionCreateResponse {
    @JsonProperty("session_id")
    public String sessionId;

    @JsonProperty("valid_until")
    public Date validUntil;
}

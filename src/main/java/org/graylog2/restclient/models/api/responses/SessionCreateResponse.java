package org.graylog2.restclient.models.api.responses;

import com.google.gson.annotations.SerializedName;

import java.util.Date;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class SessionCreateResponse {
    @SerializedName("session_id")
    public String sessionId;

    @SerializedName("valid_until")
    public Date validUntil;
}

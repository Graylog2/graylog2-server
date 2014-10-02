package org.graylog2.restclient.models.api.responses.alarmcallbacks;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class CreateAlarmCallbackResponse {
    @JsonProperty("alarmcallback_id")
    public String alarmCallbackId;
}

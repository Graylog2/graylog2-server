package org.graylog2.restclient.models.api.responses.alarmcallbacks;

import com.google.gson.annotations.SerializedName;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class CreateAlarmCallbackResponse {
    @SerializedName("alarmcallback_id")
    public String alarmCallbackId;
}

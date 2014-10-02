package org.graylog2.restclient.models.api.responses.system;

import com.google.gson.annotations.SerializedName;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class InputLaunchResponse {
    @SerializedName("input_id")
    public String inputId;

    @SerializedName("persist_id")
    public String persistId;
}

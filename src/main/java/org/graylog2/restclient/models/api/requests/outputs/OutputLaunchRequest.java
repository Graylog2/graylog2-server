package org.graylog2.restclient.models.api.requests.outputs;


import com.google.gson.annotations.SerializedName;
import org.graylog2.restclient.models.api.requests.ApiRequest;

import java.util.Map;
import java.util.Set;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class OutputLaunchRequest extends ApiRequest {
    public String title;
    public String type;
    public Map<String, Object> configuration;
    @SerializedName("creator_user_id")
    public String creatorUserId;
    @SerializedName("streams")
    public Set<String> streams;
}

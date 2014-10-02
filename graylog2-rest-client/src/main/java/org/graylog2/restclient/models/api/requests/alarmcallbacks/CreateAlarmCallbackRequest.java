package org.graylog2.restclient.models.api.requests.alarmcallbacks;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.graylog2.restclient.models.api.requests.ApiRequest;
import play.data.validation.Constraints;

import java.util.Map;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class CreateAlarmCallbackRequest extends ApiRequest {
    @Constraints.Required
    public String type;
    @Constraints.Required
    public Map<String, Object> configuration;
    @JsonProperty("creator_user_id")
    public String creatorUserId;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Map<String, Object> getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Map<String, Object> configuration) {
        this.configuration = configuration;
    }

    public String getCreatorUserId() {
        return creatorUserId;
    }

    public void setCreatorUserId(String creatorUserId) {
        this.creatorUserId = creatorUserId;
    }
}

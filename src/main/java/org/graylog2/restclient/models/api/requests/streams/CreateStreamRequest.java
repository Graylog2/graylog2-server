package org.graylog2.restclient.models.api.requests.streams;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.graylog2.restclient.models.api.requests.ApiRequest;
import play.data.validation.Constraints;

import javax.validation.Valid;
import java.util.List;

public class CreateStreamRequest extends ApiRequest {
    @Constraints.Required
    public String title;

    public String description;

    @JsonProperty("creator_user_id")
    public String creatorUserId;

    @Valid
    public List<CreateStreamRuleRequest> rules;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCreatorUserId() {
        return creatorUserId;
    }

    public void setCreatorUserId(String creatorUserId) {
        this.creatorUserId = creatorUserId;
    }

    public List<CreateStreamRuleRequest> getRules() {
        return rules;
    }

    public void setRules(List<CreateStreamRuleRequest> rules) {
        this.rules = rules;
    }
}

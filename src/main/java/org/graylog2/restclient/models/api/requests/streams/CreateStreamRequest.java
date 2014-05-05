package org.graylog2.restclient.models.api.requests.streams;

import com.google.gson.annotations.SerializedName;
import org.graylog2.restclient.models.api.requests.ApiRequest;
import play.data.validation.Constraints;

import javax.validation.Valid;
import java.util.List;

public class CreateStreamRequest extends ApiRequest {
    @Constraints.Required
    public String title;

    public String description;

    @SerializedName("creator_user_id")
    public String creatorUserId;

    @Valid
    public List<CreateStreamRuleRequest> rules;
}

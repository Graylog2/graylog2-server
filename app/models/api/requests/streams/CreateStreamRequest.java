package models.api.requests.streams;

import com.google.gson.annotations.SerializedName;
import models.api.requests.ApiRequest;
import play.data.validation.Constraints;

import javax.validation.Valid;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: dennis
 * Date: 01.11.13
 * Time: 17:04
 * To change this template use File | Settings | File Templates.
 */
public class CreateStreamRequest extends ApiRequest {
    @Constraints.Required
    public String title;

    @SerializedName("creator_user_id")
    public String creatorUserId;

    public String category;

    @Valid
    public List<CreateStreamRuleRequest> rules;
}

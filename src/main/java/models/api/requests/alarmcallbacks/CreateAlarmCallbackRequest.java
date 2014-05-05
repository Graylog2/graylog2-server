package models.api.requests.alarmcallbacks;

import com.google.gson.annotations.SerializedName;
import models.api.requests.ApiRequest;
import play.data.validation.Constraints;

import java.util.Map;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class CreateAlarmCallbackRequest extends ApiRequest {
    @Constraints.Required
    public String type;
    @Constraints.Required
    public Map<String, String> configuration;
    @SerializedName("creator_user_id")
    public String creatorUserId;
}

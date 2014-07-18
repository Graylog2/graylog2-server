package org.graylog2.restclient.models;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog2.restclient.lib.ApiClient;
import org.graylog2.restclient.models.api.responses.system.OutputSummaryResponse;

import java.util.Map;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class Output {
    private final ApiClient api;
    private final UserService userService;

    public interface Factory {
        Output fromSummaryResponse(OutputSummaryResponse outputSummaryResponse);
    }
    private final String title;
    private final String _id;
    private final String creatorUserId;
    private final Map<String, Object> configuration;
    private final String type;
    private final String createdAt;

    @AssistedInject

    public Output(ApiClient api,
                  UserService userService,
                  @Assisted OutputSummaryResponse outputSummaryResponse) {
        this.api = api;
        this.userService = userService;

        this.title = outputSummaryResponse.title;
        this._id = outputSummaryResponse._id;
        this.creatorUserId = outputSummaryResponse.creatorUserId;
        this.configuration = outputSummaryResponse.configuration;
        this.type = outputSummaryResponse.type;
        this.createdAt = outputSummaryResponse.createdAt;
    }

    public ApiClient getApi() {
        return api;
    }

    public String getTitle() {
        return title;
    }

    public String getId() {
        return _id;
    }

    public String getCreatorUserId() {
        return creatorUserId;
    }

    public User getCreatorUser() {
        return userService.load(getCreatorUserId());
    }

    public Map<String, Object> getConfiguration() {
        return configuration;
    }

    public String getType() {
        return type;
    }

    public String getCreatedAt() {
        return createdAt;
    }
}

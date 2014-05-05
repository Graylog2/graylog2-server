package org.graylog2.restclient.models;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog2.restclient.models.api.responses.alarmcallbacks.AlarmCallbackSummaryResponse;
import org.joda.time.DateTime;

import java.util.Map;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class AlarmCallback {
    public interface Factory {
        public AlarmCallback fromSummaryResponse(String streamId, AlarmCallbackSummaryResponse response);
    }

    private String id;
    private final UserService userService;
    private String streamId;
    private String type;
    private Map<String, Object> configuration;
    private DateTime createdAt;
    private String creatorUserId;
    private User creatorUser;

    @AssistedInject
    public AlarmCallback(UserService userService,
                         @Assisted String streamId,
                         @Assisted AlarmCallbackSummaryResponse response) {
        this.userService = userService;
        this.streamId = streamId;
        this.id = response.id;
        this.type = response.type;
        this.configuration = response.configuration;
        this.createdAt = DateTime.parse(response.createdAt);
        this.creatorUserId = response.creatorUserId;
        this.creatorUser = userService.load(creatorUserId);
    }

    public String getId() {
        return id;
    }

    public String getStreamId() {
        return streamId;
    }

    public String getType() {
        return type;
    }

    public Map<String, Object> getConfiguration() {
        return configuration;
    }

    public DateTime getCreatedAt() {
        return createdAt;
    }

    public String getCreatorUserId() {
        return creatorUserId;
    }

    public User getCreatorUser() {
        return creatorUser;
    }
}

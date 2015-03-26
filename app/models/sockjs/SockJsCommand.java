package models.sockjs;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonAutoDetect
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "command")
@JsonSubTypes({
        @JsonSubTypes.Type(value = CreateSessionCommand.class, name = "create_session"),
        @JsonSubTypes.Type(value = SubscribeMetricsUpdates.class, name = "metrics_subscribe"),
        @JsonSubTypes.Type(value = UnsubscribeMetricsUpdates.class, name = "metrics_unsubscribe")
})
public abstract class SockJsCommand {
    public String command;
}

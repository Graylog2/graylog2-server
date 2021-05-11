package org.graylog.plugins.views.search.export;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

@AutoValue
@JsonAutoDetect
public abstract class MessagesRequestExportJob implements ExportJob {
    static final String TYPE = "messages_export";
    private static final String FIELD_MESSAGES_REQUEST = "messages_request";

    @JsonProperty("type")
    public String type() {
        return TYPE;
    }

    @JsonProperty(FIELD_MESSAGES_REQUEST)
    public abstract MessagesRequest messagesRequest();

    static MessagesRequestExportJob fromMessagesRequest(String id, MessagesRequest messagesRequest) {
        return new AutoValue_MessagesRequestExportJob(id, messagesRequest);
    }

    @JsonCreator
    static MessagesRequestExportJob create(
            @JsonProperty(FIELD_ID) String id,
            @JsonProperty(FIELD_MESSAGES_REQUEST) MessagesRequest messagesRequest
    ) {
        return new AutoValue_MessagesRequestExportJob(id, messagesRequest);
    }
}

package org.graylog.plugins.views.search.views.widgets.messagelist;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;
import org.graylog.plugins.views.search.views.WidgetConfigDTO;

@AutoValue
@JsonTypeName(MessageListConfigDTO.NAME)
@JsonDeserialize(builder = MessageListConfigDTO.Builder.class)
public abstract class MessageListConfigDTO implements WidgetConfigDTO {
    public static final String NAME = "messages";
    private static final String FIELD_FIELDS = "fields";
    private static final String FIELD_SHOW_MESSAGE_ROW = "show_message_row";

    @JsonProperty(FIELD_FIELDS)
    public abstract ImmutableSet<String> fields();

    @JsonProperty(FIELD_SHOW_MESSAGE_ROW)
    public abstract boolean showMessageRow();

    @AutoValue.Builder
    public abstract static class Builder {
        @JsonProperty(FIELD_FIELDS)
        public abstract Builder fields(ImmutableSet<String> fields);

        @JsonProperty(FIELD_SHOW_MESSAGE_ROW)
        public abstract Builder showMessageRow(boolean showMessageRow);

        public abstract MessageListConfigDTO build();

        @JsonCreator
        public static Builder builder() {
            return new AutoValue_MessageListConfigDTO.Builder();
        }
    }
}

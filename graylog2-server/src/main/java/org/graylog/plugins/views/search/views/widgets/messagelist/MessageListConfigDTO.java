/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
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

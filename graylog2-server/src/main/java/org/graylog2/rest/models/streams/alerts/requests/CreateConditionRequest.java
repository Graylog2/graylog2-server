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
package org.graylog2.rest.models.streams.alerts.requests;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;

import javax.annotation.Nullable;
import java.util.Map;

@JsonAutoDetect
@AutoValue
@WithBeanGetter
public abstract class CreateConditionRequest {
    @JsonProperty("type")
    @Nullable
    public abstract String type();

    @JsonProperty("title")
    @Nullable
    public abstract String title();

    @JsonProperty("parameters")
    public abstract Map<String, Object> parameters();

    public static Builder builder() {
        return new AutoValue_CreateConditionRequest.Builder();
    }

    public abstract Builder toBuilder();

    @JsonCreator
    public static CreateConditionRequest create(@JsonProperty("type") @Nullable String type,
                                                @JsonProperty("title") @Nullable String title,
                                                @JsonProperty("parameters") Map<String, Object> parameters) {
        return new AutoValue_CreateConditionRequest(type, title, parameters);
    }

    @AutoValue.Builder
    public static abstract class Builder {
        public abstract Builder setType(String type);

        public abstract Builder setTitle(String title);

        public abstract Builder setParameters(Map<String, Object> parameters);

        public abstract CreateConditionRequest build();
    }
}

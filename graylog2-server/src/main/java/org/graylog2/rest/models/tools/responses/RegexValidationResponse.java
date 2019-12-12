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
package org.graylog2.rest.models.tools.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;

import java.util.Optional;

@JsonAutoDetect
@AutoValue
@WithBeanGetter
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonDeserialize(builder = AutoValue_RegexValidationResponse.Builder.class)
public abstract class RegexValidationResponse {
    @JsonProperty("regex")
    public abstract String regex();

    @JsonProperty("is_valid")
    public abstract boolean isValid();

    @JsonProperty("validation_message")
    public abstract Optional<String> validationMessage();

    public static Builder builder() {
        return new AutoValue_RegexValidationResponse.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        @JsonProperty("regex")
        public abstract Builder regex(String regex);

        @JsonProperty("is_valid")
        public abstract Builder isValid(boolean isValid);

        @JsonProperty("validation_message")
        public abstract Builder validationMessage(String validationMessage);

        public abstract RegexValidationResponse build();
    }
}

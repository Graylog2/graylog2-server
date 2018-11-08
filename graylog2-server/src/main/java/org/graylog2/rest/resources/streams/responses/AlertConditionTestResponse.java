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
package org.graylog2.rest.resources.streams.responses;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

@AutoValue
public abstract class AlertConditionTestResponse {

    @JsonProperty("triggered")
    public abstract boolean triggered();

    @JsonProperty("description")
    public abstract Optional<String> description();

    @JsonProperty("error")
    public boolean error() {
        return !errorMessages().isEmpty();
    }

    @JsonProperty("error_messages")
    public abstract ImmutableList<ErrorMessage> errorMessages();

    public static AlertConditionTestResponse create(boolean triggered, @Nullable String description) {
        return createResult(triggered, description, Collections.emptyList());
    }

    public static AlertConditionTestResponse createWithError(Throwable t) {
        return createResult(false, null, ImmutableList.of(ErrorMessage.create(t)));
    }

    private static AlertConditionTestResponse createResult(boolean triggered, @Nullable String description, Collection<ErrorMessage> errorMessages) {
        return new AutoValue_AlertConditionTestResponse(triggered, Optional.ofNullable(description), ImmutableList.copyOf(errorMessages));
    }
    @AutoValue
    public static abstract class ErrorMessage {

        @JsonProperty("type")
        public abstract String type();

        @JsonProperty("message")
        @Nullable
        public abstract String message();
        public static ErrorMessage create(Throwable t) {
            return new AutoValue_AlertConditionTestResponse_ErrorMessage(t.getClass().getCanonicalName(), t.getMessage());
        }
    }

}

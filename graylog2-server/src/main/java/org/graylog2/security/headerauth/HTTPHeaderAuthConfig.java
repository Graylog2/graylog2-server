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
package org.graylog2.security.headerauth;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;

@AutoValue
@JsonDeserialize(builder = HTTPHeaderAuthConfig.Builder.class)
public abstract class HTTPHeaderAuthConfig {
    private static final String FIELD_ENABLED = "enabled";
    private static final String FIELD_USERNAME_HEADER = "username_header";

    private static final String DEFAULT_USERNAME_HEADER = "Remote-User";

    @JsonProperty(FIELD_ENABLED)
    public abstract boolean enabled();

    @JsonProperty(FIELD_USERNAME_HEADER)
    public abstract String usernameHeader();

    public static HTTPHeaderAuthConfig createDisabled() {
        return builder().enabled(false).build();
    }

    public static Builder builder() {
        return Builder.create();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        @JsonCreator
        public static Builder create() {
            return new AutoValue_HTTPHeaderAuthConfig.Builder()
                    .enabled(false)
                    .usernameHeader(DEFAULT_USERNAME_HEADER);
        }

        @JsonProperty(FIELD_ENABLED)
        public abstract Builder enabled(boolean enabled);

        @JsonProperty(FIELD_USERNAME_HEADER)
        public abstract Builder usernameHeader(String usernameHeader);

        public abstract HTTPHeaderAuthConfig build();
    }
}

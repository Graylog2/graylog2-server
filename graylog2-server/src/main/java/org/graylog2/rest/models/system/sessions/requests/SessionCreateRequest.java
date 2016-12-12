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
package org.graylog2.rest.models.system.sessions.requests;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;
import org.hibernate.validator.constraints.NotEmpty;

@AutoValue
@WithBeanGetter
@JsonAutoDetect
public abstract class SessionCreateRequest {
    @JsonProperty
    @NotEmpty
    public abstract String username();

    @JsonProperty
    @NotEmpty
    public abstract String password();

    @JsonProperty
    public abstract String host();

    @JsonCreator
    public static SessionCreateRequest create(@JsonProperty("username") @NotEmpty String username,
                                              @JsonProperty("password") @NotEmpty String password,
                                              @JsonProperty("host") String host) {
        return new AutoValue_SessionCreateRequest(username, password, host);
    }
}

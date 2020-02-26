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
package org.graylog2.rest.models.users.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;
import org.joda.time.DateTime;

import javax.annotation.Nullable;

@JsonAutoDetect
@AutoValue
@WithBeanGetter
public abstract class Token {
    @JsonProperty
    @Nullable
    public abstract String id();

    @JsonProperty
    public abstract String name();

    @JsonProperty
    public abstract String token();

    @JsonProperty
    public abstract DateTime lastAccess();

    @JsonCreator
    public static Token create(@JsonProperty("id") @Nullable String id,
                               @JsonProperty("name") String name,
                               @JsonProperty("token") String token,
                               @JsonProperty("last_access") DateTime lastAccess) {
        return new AutoValue_Token(id, name, token, lastAccess);
    }
}

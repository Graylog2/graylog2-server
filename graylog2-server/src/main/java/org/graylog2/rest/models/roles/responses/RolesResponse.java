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
package org.graylog2.rest.models.roles.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;

import javax.validation.constraints.NotNull;
import java.util.Set;

@AutoValue
@WithBeanGetter
@JsonAutoDetect
public abstract class RolesResponse {

    @JsonProperty
    @NotNull
    public abstract Set<RoleResponse> roles();

    @JsonProperty
    public int total() {
        return roles().size();
    }

    @JsonCreator
    public static RolesResponse create(@JsonProperty("roles") @NotNull Set<RoleResponse> roles) {
        return new AutoValue_RolesResponse(roles);
    }
}

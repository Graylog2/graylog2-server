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
import com.google.common.base.Optional;
import org.graylog.autovalue.WithBeanGetter;
import org.hibernate.validator.constraints.NotBlank;

import javax.validation.constraints.NotNull;
import java.util.Set;

@AutoValue
@WithBeanGetter
@JsonAutoDetect
public abstract class RoleResponse {

    @JsonProperty
    @NotBlank
    public abstract String name();

    @JsonProperty
    public abstract Optional<String> description();

    @JsonProperty
    @NotNull
    public abstract Set<String> permissions();

    @JsonProperty
    public abstract boolean readOnly();

    @JsonCreator
    public static RoleResponse create(@JsonProperty("name") @NotBlank String name,
                                      @JsonProperty("description") Optional<String> description,
                                      @JsonProperty("permissions") @NotNull Set<String> permissions,
                                      @JsonProperty("read_only") boolean readOnly) {
        return new AutoValue_RoleResponse(name, description, permissions, readOnly);
    }
}

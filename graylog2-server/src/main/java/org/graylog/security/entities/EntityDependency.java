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
package org.graylog.security.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;
import org.graylog.grn.GRN;

import java.util.Objects;

@AutoValue
public abstract class EntityDependency {
    @JsonProperty("id")
    public abstract GRN id();

    @JsonProperty("title")
    public abstract String title();

    @JsonProperty("owners")
    public abstract ImmutableSet<String> owners();

    @JsonCreator
    public static EntityDependency create(@JsonProperty("id") GRN id,
                                          @JsonProperty("title") String title,
                                          @JsonProperty("owners") ImmutableSet<String> owners) {
        return new AutoValue_EntityDependency(id, Objects.toString(title, "<no title>"), owners);
    }
}

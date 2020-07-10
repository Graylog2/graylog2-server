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
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;
import org.graylog.grn.GRN;

import java.util.Collections;
import java.util.Set;

@AutoValue
@JsonDeserialize(builder = EntityDependency.Builder.class)
public abstract class EntityDependency {
    @JsonProperty("id")
    public abstract GRN id();

    @JsonProperty("type")
    public String type() {
        return id().type();
    }

    @JsonProperty("title")
    public abstract String title();

    @JsonProperty("owners")
    public abstract ImmutableSet<Owner> owners();

    public static EntityDependency create(GRN id, String title, Set<Owner> owners) {
        return builder()
                .id(id)
                .title(title)
                .owners(owners)
                .build();
    }

    public static Builder builder() {
        return Builder.create();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public abstract static class Builder {
        @JsonCreator
        public static Builder create() {
            return new AutoValue_EntityDependency.Builder().owners(Collections.emptySet());
        }

        @JsonProperty("id")
        public abstract Builder id(GRN id);

        @JsonProperty("title")
        public abstract Builder title(String title);

        @JsonProperty("owners")
        public abstract Builder owners(Set<Owner> owners);

        public abstract EntityDependency build();
    }

    @AutoValue
    public static abstract class Owner {
        @JsonProperty("id")
        public abstract GRN id();

        @JsonProperty("type")
        public String type() {
            return id().type();
        }

        @JsonProperty("title")
        public abstract String title();

        @JsonCreator
        public static Owner create(GRN id, String title) {
            return new AutoValue_EntityDependency_Owner(id, title);
        }
    }
}

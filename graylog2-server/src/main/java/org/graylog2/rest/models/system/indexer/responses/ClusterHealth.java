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
package org.graylog2.rest.models.system.indexer.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;

@JsonAutoDetect
@AutoValue
@WithBeanGetter
public abstract class ClusterHealth {
    @JsonProperty
    public abstract String status();

    @JsonProperty
    public abstract ShardStatus shards();

    @JsonCreator
    public static ClusterHealth create(@JsonProperty("status") String status,
                                       @JsonProperty("shards") ShardStatus shards) {
        return new AutoValue_ClusterHealth(status, shards);
    }

    @JsonAutoDetect
    @AutoValue
    @WithBeanGetter
    public static abstract class ShardStatus {
        @JsonProperty
        public abstract int active();

        @JsonProperty
        public abstract int initializing();

        @JsonProperty
        public abstract int relocating();

        @JsonProperty
        public abstract int unassigned();

        @JsonCreator
        public static ShardStatus create(@JsonProperty("active") int active,
                                         @JsonProperty("initializing") int initializing,
                                         @JsonProperty("relocating") int relocating,
                                         @JsonProperty("unassigned") int unassigned) {
            return new AutoValue_ClusterHealth_ShardStatus(active, initializing, relocating, unassigned);
        }
    }
}

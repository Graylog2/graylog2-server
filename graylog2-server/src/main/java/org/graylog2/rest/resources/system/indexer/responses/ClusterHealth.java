/**
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.rest.resources.system.indexer.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

@JsonAutoDetect
@AutoValue
public abstract class ClusterHealth {
    @JsonProperty
    public abstract String status();

    @JsonProperty
    public abstract ShardStatus shards();

    public static ClusterHealth create(String status, ShardStatus shards) {
        return new AutoValue_ClusterHealth(status, shards);
    }

    @JsonAutoDetect
    @AutoValue
    public static abstract class ShardStatus {
        @JsonProperty
        public abstract int active();

        @JsonProperty
        public abstract int initializing();

        @JsonProperty
        public abstract int relocating();

        @JsonProperty
        public abstract int unassigned();

        public static ShardStatus create(int active, int initializing, int relocating, int unassigned) {
            return new AutoValue_ClusterHealth_ShardStatus(active, initializing, relocating, unassigned);
        }
    }
}

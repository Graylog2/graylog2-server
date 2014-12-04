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

import java.util.List;

@JsonAutoDetect
@AutoValue
public abstract class IndexInfo {
    @JsonProperty
    public abstract IndexStats primaryShards();

    @JsonProperty
    public abstract IndexStats allShards();

    @JsonProperty
    public abstract List<ShardRouting> routing();

    @JsonProperty
    public abstract boolean isReopened();

    public static IndexInfo create(IndexStats primaryShards,
                                   IndexStats allShards,
                                   List<ShardRouting> routing,
                                   boolean isReopened) {
        return new AutoValue_IndexInfo(primaryShards, allShards, routing, isReopened);
    }
}

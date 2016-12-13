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

import java.util.List;

@JsonAutoDetect
@AutoValue
@WithBeanGetter
public abstract class IndexInfo {
    @JsonProperty
    public abstract IndexStats primaryShards();

    @JsonProperty
    public abstract IndexStats allShards();

    @JsonProperty
    public abstract List<ShardRouting> routing();

    @JsonProperty
    public abstract boolean isReopened();

    @JsonCreator
    public static IndexInfo create(@JsonProperty("primary_shards") IndexStats primaryShards,
                                   @JsonProperty("all_shards") IndexStats allShards,
                                   @JsonProperty("routing") List<ShardRouting> routing,
                                   @JsonProperty("is_reopened") boolean isReopened) {
        return new AutoValue_IndexInfo(primaryShards, allShards, routing, isReopened);
    }
}

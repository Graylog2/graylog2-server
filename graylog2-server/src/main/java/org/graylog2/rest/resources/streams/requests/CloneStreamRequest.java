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
package org.graylog2.rest.resources.streams.requests;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;

@JsonAutoDetect
@AutoValue
@WithBeanGetter
public abstract class CloneStreamRequest {
    @JsonProperty
    public abstract String title();

    @JsonProperty
    public abstract String description();

    @JsonProperty("remove_matches_from_default_stream")
    public abstract boolean removeMatchesFromDefaultStream();

    @JsonProperty("index_set_id")
    public abstract String indexSetId();

    @JsonCreator
    public static CloneStreamRequest create(@JsonProperty("title") String title,
                                            @JsonProperty("description") String description,
                                            @JsonProperty("remove_matches_from_default_stream") boolean removeMatchesFromDefaultStream,
                                            @JsonProperty("index_set_id") String indexSetId) {
        return new AutoValue_CloneStreamRequest(title, description, removeMatchesFromDefaultStream, indexSetId);
    }
}

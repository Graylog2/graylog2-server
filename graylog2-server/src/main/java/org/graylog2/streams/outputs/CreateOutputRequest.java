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
package org.graylog2.streams.outputs;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;

@JsonAutoDetect
@AutoValue
public abstract class CreateOutputRequest {
    @JsonProperty
    public abstract String title();
    @JsonProperty
    public abstract String type();
    @JsonProperty
    public abstract Map<String, Object> configuration();
    @JsonProperty
    public abstract Set<String> streams();
    @JsonProperty
    @Nullable
    public abstract String contentPack();

    public static CreateOutputRequest create(@JsonProperty("title") String title,
                                             @JsonProperty("type") String type,
                                             @JsonProperty("configuration") Map<String, Object> configuration,
                                             @JsonProperty("streams") Set<String> streams,
                                             @JsonProperty("content_pack") @Nullable String contentPack) {
        return new AutoValue_CreateOutputRequest(title, type, configuration, streams, contentPack);
    }

    public static CreateOutputRequest create(@JsonProperty("title") String title,
                                             @JsonProperty("type") String type,
                                             @JsonProperty("configuration") Map<String, Object> configuration,
                                             @JsonProperty("streams") Set<String> streams) {
        return create(title, type, configuration, streams, null);
    }
}

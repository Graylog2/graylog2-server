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
package org.graylog2.contentpacks.model.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.contentpacks.model.entities.references.ReferenceMap;
import org.graylog2.contentpacks.model.entities.references.ValueReference;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@AutoValue
@WithBeanGetter
public abstract class StreamAlarmCallbackEntity {
    @JsonProperty("type")
    @NotBlank
    public abstract String type();

    @JsonProperty("title")
    @NotBlank
    public abstract ValueReference title();

    @JsonProperty("stream_id")
    public abstract String streamId();

    @JsonProperty("configuration")
    @NotNull
    public abstract ReferenceMap configuration();
    @JsonCreator
    public static StreamAlarmCallbackEntity create(@JsonProperty("type") @NotBlank String type,
                                                   @JsonProperty("title") @NotBlank ValueReference title,
                                                   @JsonProperty("stream_id") @NotBlank String streamId,
                                                   @JsonProperty("configuration") @NotNull ReferenceMap configuration) {
        return new AutoValue_StreamAlarmCallbackEntity(type, title, streamId, configuration);
    }
}
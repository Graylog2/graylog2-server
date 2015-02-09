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
package org.graylog2.rest.models.system.inputs.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.joda.time.DateTime;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * Created by dennis on 12/12/14.
 */
@JsonAutoDetect
@AutoValue
public abstract class InputSummary {
    @JsonProperty
    public abstract String title();
    @JsonProperty
    public abstract String persistId();
    @JsonProperty
    public abstract Boolean global();
    @JsonProperty
    public abstract String name();
    @JsonProperty
    @Nullable
    public abstract String contentPack();
    @JsonProperty
    public abstract String inputId();
    @JsonProperty
    public abstract DateTime createdAt();
    @JsonProperty
    public abstract String type();
    @JsonProperty
    public abstract String creatorUserId();
    @JsonProperty
    public abstract Map<String, Object> attributes();
    @JsonProperty
    public abstract Map<String, String> staticFields();

    public static InputSummary create(String title,
                                      String persistId,
                                      Boolean global,
                                      String name,
                                      @Nullable String contentPack,
                                      String inputId,
                                      DateTime createdAt,
                                      String type,
                                      String creatorUserId,
                                      Map<String, Object> attributes,
                                      Map<String, String> staticFields) {
        return new AutoValue_InputSummary(title, persistId, global, name, contentPack, inputId, createdAt, type, creatorUserId, attributes, staticFields);
    }
}

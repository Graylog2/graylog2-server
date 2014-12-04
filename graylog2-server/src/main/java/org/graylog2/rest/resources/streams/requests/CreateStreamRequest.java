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
package org.graylog2.rest.resources.streams.requests;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog2.rest.resources.streams.rules.requests.CreateStreamRuleRequest;

import javax.annotation.Nullable;
import java.util.List;

@JsonAutoDetect
@AutoValue
public abstract class CreateStreamRequest {
    @JsonProperty
    public abstract String title();

    @JsonProperty
    public abstract String description();

    @JsonProperty
    public abstract List<CreateStreamRuleRequest> rules();

    @JsonProperty
    @Nullable
    public abstract String contentPack();

    @JsonCreator
    public static CreateStreamRequest create(@JsonProperty("title") String title,
                                             @JsonProperty("description") String description,
                                             @JsonProperty("rules") List<CreateStreamRuleRequest> rules,
                                             @JsonProperty("content_pack") @Nullable String contentPack) {
        return new AutoValue_CreateStreamRequest(title, description, rules, contentPack);
    }
}

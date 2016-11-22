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
import org.graylog2.plugin.streams.Stream;
import org.graylog2.rest.resources.streams.rules.requests.CreateStreamRuleRequest;
import org.hibernate.validator.constraints.NotEmpty;

import javax.annotation.Nullable;
import java.util.List;

import static com.google.common.base.MoreObjects.firstNonNull;

@JsonAutoDetect
@AutoValue
public abstract class CreateStreamRequest {
    @JsonProperty
    public abstract String title();

    @JsonProperty
    @Nullable
    public abstract String description();

    @JsonProperty
    @Nullable
    public abstract List<CreateStreamRuleRequest> rules();

    @JsonProperty
    @Nullable
    public abstract String contentPack();

    @JsonProperty
    public abstract Stream.MatchingType matchingType();

    @JsonProperty("remove_from_all_messages")
    public abstract boolean removeFromAllMessages();

    @JsonCreator
    public static CreateStreamRequest create(@JsonProperty("title") @NotEmpty String title,
                                             @JsonProperty("description") @Nullable String description,
                                             @JsonProperty("rules") @Nullable List<CreateStreamRuleRequest> rules,
                                             @JsonProperty("content_pack") @Nullable String contentPack,
                                             @JsonProperty("matching_type") @Nullable String matchingType,
                                             @JsonProperty("remove_from_all_messages") @Nullable Boolean removeFromAllMessages) {
        return new AutoValue_CreateStreamRequest(
                title,
                description,
                rules,
                contentPack,
                Stream.MatchingType.valueOfOrDefault(matchingType),
                firstNonNull(removeFromAllMessages, false)
        );
    }
}

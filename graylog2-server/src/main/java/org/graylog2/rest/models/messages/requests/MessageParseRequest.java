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
package org.graylog2.rest.models.messages.requests;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.sun.istack.internal.Nullable;

import java.util.Map;

@JsonAutoDetect
@AutoValue
public abstract class MessageParseRequest {
    @JsonProperty
    public abstract String message();

    @JsonProperty
    public abstract String codec();

    @JsonProperty
    public abstract String remoteAddress();

    @JsonProperty
    @Nullable
    public abstract Map<String, Object> configuration();

    @JsonCreator
    public static MessageParseRequest create(@JsonProperty("message") String message,
                                             @JsonProperty("codec") String codec,
                                             @JsonProperty("remote_address") String remoteAddress,
                                             @JsonProperty("configuration") Map<String, Object> configuration) {
        return new AutoValue_MessageParseRequest(message, codec, remoteAddress, configuration);
    }
}

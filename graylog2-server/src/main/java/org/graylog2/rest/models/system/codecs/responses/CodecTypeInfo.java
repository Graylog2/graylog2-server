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
package org.graylog2.rest.models.system.codecs.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog2.plugin.configuration.ConfigurationRequest;

import java.util.Map;

@AutoValue
@JsonAutoDetect
public abstract class CodecTypeInfo {
    @JsonProperty
    public abstract String type();

    @JsonProperty
    public abstract String name();

    @JsonProperty
    public abstract Map<String, Map<String, Object>> requestedConfiguration();

    @JsonCreator
    public static CodecTypeInfo create(@JsonProperty("type") String type,
                                       @JsonProperty("name") String name,
                                       @JsonProperty("requested_configuration") Map<String, Map<String, Object>> requestedConfiguration) {
        return new AutoValue_CodecTypeInfo(type, name, requestedConfiguration);
    }

    public static CodecTypeInfo fromConfigurationRequest(String type, String name, ConfigurationRequest configurationRequest) {
        return create(type, name, configurationRequest.asList());
    }
}

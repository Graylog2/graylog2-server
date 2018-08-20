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
package org.graylog.plugins.sidecar.rest.responses;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

import java.util.Collection;

@AutoValue
public abstract class ConfigurationSidecarsResponse {
    @JsonProperty("configuration_id")
    public abstract String configurationId();

    @JsonProperty("sidecar_ids")
    public abstract Collection<String> sidecarIds();

    @JsonCreator
    public static ConfigurationSidecarsResponse create(@JsonProperty("configuration_id") String configurationId,
                                                       @JsonProperty("sidecar_ids") Collection<String> sidecarIds) {
        return new AutoValue_ConfigurationSidecarsResponse(configurationId, sidecarIds);
    }

}

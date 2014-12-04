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
package org.graylog2.rest.resources.streams.outputs;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog2.plugin.configuration.ConfigurationRequest;

@JsonAutoDetect
@AutoValue
public abstract class AvailableOutputSummary {
    @JsonProperty
    public abstract String name();

    @JsonProperty
    public abstract String type();

    @JsonProperty("human_name")
    public abstract String humanName();

    @JsonProperty("link_to_docs")
    public abstract String linkToDocs();

    @JsonProperty
    public abstract ConfigurationRequest requestedConfiguration();

    public static AvailableOutputSummary create(String name, String type, String humanName, String linkToDocs, ConfigurationRequest requestedConfiguration) {
        return new AutoValue_AvailableOutputSummary(name, type, humanName, linkToDocs, requestedConfiguration);
    }
}

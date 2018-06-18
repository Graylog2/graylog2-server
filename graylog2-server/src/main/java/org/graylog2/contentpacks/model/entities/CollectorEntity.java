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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.contentpacks.model.entities.references.ValueReference;

import java.util.List;

@JsonAutoDetect
@AutoValue
@WithBeanGetter
public abstract class CollectorEntity {
    @JsonProperty("title")
    public abstract ValueReference title();

    @JsonProperty("service_type")
    public abstract ValueReference serviceType();

    @JsonProperty("node_operating_system")
    public abstract ValueReference nodeOperatingSystem();

    @JsonProperty("executable_path")
    public abstract ValueReference executablePath();

    @JsonProperty("configuration_path")
    public abstract ValueReference configurationPath();

    @JsonProperty("execute_parameters")
    public abstract List<ValueReference> executeParameters();

    @JsonProperty("validation_command")
    public abstract List<ValueReference> validationCommand();

    @JsonProperty("default_template")
    public abstract ValueReference defaultTemplate();

    @JsonCreator
    public static CollectorEntity create(@JsonProperty("title") ValueReference title,
                                         @JsonProperty("service_type") ValueReference serviceType,
                                         @JsonProperty("node_operating_system") ValueReference nodeOperatingSystem,
                                         @JsonProperty("executable_path") ValueReference executablePath,
                                         @JsonProperty("configuration_path") ValueReference configurationPath,
                                         @JsonProperty("execute_parameters") List<ValueReference> executeParameters,
                                         @JsonProperty("validation_command") List<ValueReference> validationCommand,
                                         @JsonProperty("default_template") ValueReference defaultTemplate) {
        return new AutoValue_CollectorEntity(title,
                serviceType,
                nodeOperatingSystem,
                executablePath,
                configurationPath,
                executeParameters,
                validationCommand,
                defaultTemplate);
    }
}

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
package org.graylog.plugins.sidecar.rest.models;

import com.fasterxml.jackson.annotation.*;
import com.google.auto.value.AutoValue;
import org.mongojack.Id;
import org.mongojack.ObjectId;

import javax.annotation.Nullable;

@AutoValue
@JsonAutoDetect
public abstract class Collector {
    public static final String FIELD_ID = "id";
    public static final String FIELD_NAME = "name";
    public static final String FIELD_SERVICE_TYPE = "service_type";
    public static final String FIELD_NODE_OPERATING_SYSTEM = "node_operating_system";
    public static final String FIELD_EXECUTABLE_PATH = "executable_path";
    public static final String FIELD_EXECUTE_PARAMETERS = "execute_parameters";
    public static final String FIELD_VALIDATION_PARAMETERS = "validation_parameters";
    public static final String FIELD_DEFAULT_TEMPLATE = "default_template";

    @Id
    @ObjectId
    @Nullable
    @JsonProperty(FIELD_ID)
    public abstract String id();

    @JsonProperty(FIELD_NAME)
    public abstract String name();

    // exec, svc, systemd, ...
    @JsonProperty(FIELD_SERVICE_TYPE)
    public abstract String serviceType();

    @JsonProperty(FIELD_NODE_OPERATING_SYSTEM)
    public abstract String nodeOperatingSystem();

    @JsonProperty(FIELD_EXECUTABLE_PATH)
    public abstract String executablePath();

    @JsonProperty(FIELD_EXECUTE_PARAMETERS)
    @Nullable
    public abstract String executeParameters();

    @JsonProperty(FIELD_VALIDATION_PARAMETERS)
    @Nullable
    public abstract String validationParameters();

    @JsonProperty(FIELD_DEFAULT_TEMPLATE)
    public abstract String defaultTemplate();

    public static Builder builder() {
        return new AutoValue_Collector.Builder();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder id(String id);
        public abstract Builder name(String value);
        public abstract Builder serviceType(String serviceType);
        public abstract Builder nodeOperatingSystem(String nodeOperatingSystem);
        public abstract Builder executablePath(String executablePath);
        public abstract Builder executeParameters(String executeParameters);
        public abstract Builder validationParameters(String validationParameters);
        public abstract Builder defaultTemplate(String defaultTemplate);
        public abstract Collector build();
    }

    @JsonCreator
    public static Collector create(@JsonProperty(FIELD_ID) @Nullable String id,
                                   @JsonProperty(FIELD_NAME) String name,
                                   @JsonProperty(FIELD_SERVICE_TYPE) String serviceType,
                                   @JsonProperty(FIELD_NODE_OPERATING_SYSTEM) String nodeOperatingSystem,
                                   @JsonProperty(FIELD_EXECUTABLE_PATH) String executablePath,
                                   @JsonProperty(FIELD_EXECUTE_PARAMETERS) @Nullable String executeParameters,
                                   @JsonProperty(FIELD_VALIDATION_PARAMETERS) @Nullable String validationParameters,
                                   @JsonProperty(FIELD_DEFAULT_TEMPLATE) String defaultTemplate) {
        return builder()
                .id(id)
                .name(name)
                .serviceType(serviceType)
                .nodeOperatingSystem(nodeOperatingSystem)
                .executablePath(executablePath)
                .executeParameters(executeParameters)
                .validationParameters(validationParameters)
                .defaultTemplate(defaultTemplate)
                .build();
    }
}

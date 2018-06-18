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

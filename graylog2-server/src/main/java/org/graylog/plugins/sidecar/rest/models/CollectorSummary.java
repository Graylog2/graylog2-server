package org.graylog.plugins.sidecar.rest.models;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

@AutoValue
@JsonAutoDetect
public abstract class CollectorSummary {
    @JsonProperty("id")
    public abstract String id();

    @JsonProperty("name")
    public abstract String name();

    @JsonProperty("service_type")
    public abstract String serviceType();

    @JsonProperty("node_operating_system")
    public abstract String nodeOperatingSystem();

    @JsonProperty("default_template")
    public abstract String defaultTemplate();

    @JsonCreator
    public static CollectorSummary create(@JsonProperty("id") String id,
                                          @JsonProperty("name") String name,
                                          @JsonProperty("service_type") String serviceType,
                                          @JsonProperty("node_operating_system") String nodeOperatingSystem,
                                          @JsonProperty("default_template") String defaultTemplate) {
        return new AutoValue_CollectorSummary(id, name, serviceType, nodeOperatingSystem, defaultTemplate);
    }

    public static CollectorSummary create(Collector collector) {
        return create(
                collector.id(),
                collector.name(),
                collector.serviceType(),
                collector.nodeOperatingSystem(),
                collector.defaultTemplate());
    }
}

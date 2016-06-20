package org.graylog2.decorators;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.decorators.MessageDecorator;

@AutoValue
@JsonAutoDetect
public abstract class DecoratorTypeInfo {
    private static final String FIELD_TYPE = "type";
    private static final String FIELD_NAME = "name";
    private static final String FIELD_HUMAN_NAME = "human_name";
    private static final String FIELD_IS_EXCLUSIVE = "is_exlusive";
    private static final String FIELD_REQUESTED_CONFIGURATION = "requested_configuration";
    private static final String FIELD_LINK_TO_DOCS = "link_to_docs";

    @JsonProperty(FIELD_TYPE)
    public abstract String type();
    @JsonProperty(FIELD_NAME)
    public abstract String name();
    @JsonProperty(FIELD_HUMAN_NAME)
    public abstract String humanName();
    @JsonProperty(FIELD_IS_EXCLUSIVE)
    public abstract boolean isExclusive();
    @JsonProperty(FIELD_REQUESTED_CONFIGURATION)
    public abstract ConfigurationRequest requestedConfiguration();
    @JsonProperty(FIELD_LINK_TO_DOCS)
    public abstract String linkToDocs();

    @JsonCreator
    public static DecoratorTypeInfo create(@JsonProperty(FIELD_TYPE) String type,
                                           @JsonProperty(FIELD_NAME) String name,
                                           @JsonProperty(FIELD_HUMAN_NAME) String humanName,
                                           @JsonProperty(FIELD_IS_EXCLUSIVE) boolean isExclusive,
                                           @JsonProperty(FIELD_REQUESTED_CONFIGURATION) ConfigurationRequest requestedConfiguration,
                                           @JsonProperty(FIELD_LINK_TO_DOCS) String linkToDocs) {
        return new AutoValue_DecoratorTypeInfo(type, name, humanName, isExclusive, requestedConfiguration, linkToDocs);
    }

    public static DecoratorTypeInfo create(String type, MessageDecorator.Descriptor descriptor, ConfigurationRequest requestedConfiguration) {
        return create(type, descriptor.getName(), descriptor.getHumanName(), descriptor.isExclusive(), requestedConfiguration, descriptor.getLinkToDocs());
    }
}

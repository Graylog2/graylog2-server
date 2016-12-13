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
package org.graylog2.plugin.configuration;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.plugin.DescriptorWithHumanName;

@AutoValue
@WithBeanGetter
@JsonAutoDetect
public abstract class ConfigurableTypeInfo {
    private static final String FIELD_TYPE = "type";
    private static final String FIELD_NAME = "name";
    private static final String FIELD_HUMAN_NAME = "human_name";
    private static final String FIELD_REQUESTED_CONFIGURATION = "requested_configuration";
    private static final String FIELD_LINK_TO_DOCS = "link_to_docs";

    @JsonProperty(FIELD_TYPE)
    public abstract String type();
    @JsonProperty(FIELD_NAME)
    public abstract String name();
    @JsonProperty(FIELD_HUMAN_NAME)
    public abstract String humanName();
    @JsonProperty(FIELD_REQUESTED_CONFIGURATION)
    public abstract ConfigurationRequest requestedConfiguration();
    @JsonProperty(FIELD_LINK_TO_DOCS)
    public abstract String linkToDocs();

    @JsonCreator
    public static ConfigurableTypeInfo create(@JsonProperty(FIELD_TYPE) String type,
                                              @JsonProperty(FIELD_NAME) String name,
                                              @JsonProperty(FIELD_HUMAN_NAME) String humanName,
                                              @JsonProperty(FIELD_REQUESTED_CONFIGURATION) ConfigurationRequest requestedConfiguration,
                                              @JsonProperty(FIELD_LINK_TO_DOCS) String linkToDocs) {
        return new AutoValue_ConfigurableTypeInfo(type, name, humanName, requestedConfiguration, linkToDocs);
    }

    public static ConfigurableTypeInfo create(String type, DescriptorWithHumanName descriptor, ConfigurationRequest requestedConfiguration) {
        return create(type, descriptor.getName(), descriptor.getHumanName(), requestedConfiguration, descriptor.getLinkToDocs());
    }
}

package org.graylog2.contentpacks.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.contentpacks.model.entities.NativeEntityDescriptor;

import java.util.Set;

@AutoValue
@WithBeanGetter
@JsonAutoDetect
public abstract class ContentPackUninstallDetails {
    public static final String FIELD_ENTITES = "entities";

    @JsonProperty(FIELD_ENTITES)
    public abstract Set<NativeEntityDescriptor> entities();

    public static ContentPackUninstallDetails create(@JsonProperty(FIELD_ENTITES) Set<NativeEntityDescriptor> entities) {
        return new AutoValue_ContentPackUninstallDetails(entities);
    }
}

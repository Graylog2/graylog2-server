package org.graylog2.rest.models.system.contenpacks.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;

import java.util.Map;

@JsonAutoDetect

@AutoValue
@WithBeanGetter
public abstract class ContentPackMetaData {
    @JsonProperty
    public abstract Map<Integer, Integer> installationCount();

    @JsonCreator
    public static ContentPackMetaData create(@JsonProperty("installation_count") Map<Integer, Integer> installationCount) {
        return new AutoValue_ContentPackMetaData(installationCount);
    }
}

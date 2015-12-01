package org.graylog2.web;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

import java.util.Map;

@AutoValue
@JsonAutoDetect
public abstract class PackageManifest {
    @JsonProperty("files")
    public abstract Map<String, Object> files();

    @JsonCreator
    public static PackageManifest create(@JsonProperty("files") Map<String, Object> files) {
        return new AutoValue_PackageManifest(files);
    }
}

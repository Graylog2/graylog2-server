package org.graylog2.web;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

import java.util.List;
import java.util.Map;

@AutoValue
@JsonAutoDetect
public abstract class PackageFiles {
    @JsonProperty("chunks")
    public abstract Map<String, ChunkDescription> chunks();

    @JsonProperty("js")
    public abstract List<String> jsFiles();

    @JsonProperty("css")
    public abstract List<String> cssFiles();

    @JsonCreator
    public static PackageFiles create(@JsonProperty("chunks") Map<String, ChunkDescription> chunks,
                                      @JsonProperty("js") List<String> jsFiles,
                                      @JsonProperty("css") List<String> cssFiles) {
        return new AutoValue_PackageFiles(chunks, jsFiles, cssFiles);
    }
}

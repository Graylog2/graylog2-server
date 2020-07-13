package org.graylog.storage.elasticsearch7.cat;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

import javax.annotation.Nullable;

@AutoValue
@JsonAutoDetect
public abstract class NodeResponse {
    public abstract String id();

    public abstract String name();

    @Nullable
    public abstract String host();

    public abstract String ip();

    public abstract String diskUsed();

    public abstract String diskTotal();

    public abstract Double diskUsedPercent();

    public abstract Long fileDescriptorMax();

    @JsonCreator
    public static NodeResponse create(@JsonProperty("id") String id,
                                      @JsonProperty("name") String name,
                                      @JsonProperty("host") @Nullable String host,
                                      @JsonProperty("ip") String ip,
                                      @JsonProperty("diskUsed") String diskUsed,
                                      @JsonProperty("diskTotal") String diskTotal,
                                      @JsonProperty("diskUsedPercent") Double diskUsedPercent,
                                      @JsonProperty("fileDescriptorMax") Long fileDescriptorMax) {
        return new AutoValue_NodeResponse(
                id,
                name,
                host,
                ip,
                diskUsed,
                diskTotal,
                diskUsedPercent,
                fileDescriptorMax
        );
    }
}

package org.graylog2.system.stats.elasticsearch;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

import java.util.List;

@JsonAutoDetect
@AutoValue
public abstract class NodeInfo {

    public static Builder builder() {
        return new AutoValue_NodeInfo.Builder();
    }

    @JsonProperty
    public abstract String version();

    @JsonProperty
    public abstract Object os();

    @JsonProperty
    public abstract Long jvmMemHeapMaxInBytes();

    @JsonProperty
    public abstract List<String> roles();

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder version(String version);

        public abstract Builder os(Object os);

        public abstract Builder jvmMemHeapMaxInBytes(Long jvmMemHeapMaxInBytes);

        public abstract Builder roles(List<String> roles);

        public abstract NodeInfo build();
    }
}

package org.graylog.plugins.messageprocessor.ast;

import com.google.auto.value.AutoValue;

import java.util.List;

@AutoValue
public abstract class Pipeline {

    public abstract String name();
    public abstract List<Stage> stages();

    public static Builder builder() {
        return new AutoValue_Pipeline.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Pipeline build();

        public abstract Builder name(String name);

        public abstract Builder stages(List<Stage> stages);
    }
}

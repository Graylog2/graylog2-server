package org.graylog.plugins.messageprocessor.ast;

import com.google.auto.value.AutoValue;

import java.util.List;

@AutoValue
public abstract class Stage {

    public abstract int stage();
    public abstract boolean matchAll();
    public abstract List<String> ruleReferences();

    public static Builder builder() {
        return new AutoValue_Stage.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Stage build();

        public abstract Builder stage(int stageNumber);

        public abstract Builder matchAll(boolean mustMatchAll);

        public abstract Builder ruleReferences(List<String> ruleRefs);
    }
}

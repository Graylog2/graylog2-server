package org.graylog.plugins.enterprise.search.views;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog.plugins.enterprise.search.views.formatting.highlighting.HighlightingRule;

import java.util.Collections;
import java.util.Set;

@AutoValue
@JsonDeserialize(builder = FormattingSettings.Builder.class)
@WithBeanGetter
public abstract class FormattingSettings {
    static final String FIELD_HIGHLIGHTING = "highlighting";

    @JsonProperty(FIELD_HIGHLIGHTING)
    public abstract Set<HighlightingRule> highlighting();

    @AutoValue.Builder
    public static abstract class Builder {
        @JsonProperty(FIELD_HIGHLIGHTING)
        public abstract Builder highlighting(Set<HighlightingRule> highlightingRules);

        public abstract FormattingSettings build();

        @JsonCreator
        public static Builder create() {
            return new AutoValue_FormattingSettings.Builder().highlighting(Collections.emptySet());
        }
    }
}

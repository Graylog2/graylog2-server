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
package org.graylog.plugins.views.search.views;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog.plugins.views.search.views.formatting.highlighting.HighlightingRule;

import java.util.Collections;
import java.util.Set;

@AutoValue
@JsonDeserialize(builder = FormattingSettings.Builder.class)
@WithBeanGetter
public abstract class FormattingSettings {
    static final String FIELD_HIGHLIGHTING = "highlighting";

    @JsonProperty(FIELD_HIGHLIGHTING)
    public abstract Set<HighlightingRule> highlighting();

    public static Builder builder() {
        return Builder.create();
    }

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

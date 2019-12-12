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
package org.graylog.plugins.views.migrations.V20191203120602_MigrateSavedSearchesToViewsSupport.view;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

@AutoValue
public abstract class KeywordRange extends TimeRange {
    static final String KEYWORD = "keyword";

    @JsonProperty
    @Override
    public abstract String type();

    @JsonProperty
    abstract String keyword();

    @JsonCreator
    static KeywordRange create(@JsonProperty("type") String type, @JsonProperty("keyword") String keyword) {
        return builder().type(type).keyword(keyword).build();
    }

    public static KeywordRange create(String keyword) {
        return create(KEYWORD, keyword);
    }

    private static Builder builder() {
        return new AutoValue_KeywordRange.Builder();
    }

    String getKeyword() {
        return keyword();
    }

    @AutoValue.Builder
    abstract static class Builder {
        abstract Builder type(String type);

        abstract Builder keyword(String keyword);

        abstract String keyword();

        abstract KeywordRange build();
    }
}


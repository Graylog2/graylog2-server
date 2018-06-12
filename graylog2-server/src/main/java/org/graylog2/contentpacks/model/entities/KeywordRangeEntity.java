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
package org.graylog2.contentpacks.model.entities;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog2.contentpacks.model.entities.references.ValueReference;
import org.graylog2.plugin.indexer.searches.timeranges.KeywordRange;

@AutoValue
@JsonAutoDetect
@JsonDeserialize(builder = AutoValue_KeywordRangeEntity.Builder.class)
public abstract class KeywordRangeEntity extends TimeRangeEntity {
    static final String TYPE = "keyword";
    private static final String FIELD_KEYWORD = "keyword";

    @JsonProperty(FIELD_KEYWORD)
    public abstract ValueReference keyword();

    public static KeywordRangeEntity of(KeywordRange keywordRange) {
        final String keyword = keywordRange.keyword();
        return builder()
                .keyword(ValueReference.of(keyword))
                .build();
    }

    static KeywordRangeEntity.Builder builder() {
        return new AutoValue_KeywordRangeEntity.Builder();
    }

    @AutoValue.Builder
    abstract static class Builder implements TimeRangeBuilder<Builder> {
        @JsonProperty(FIELD_KEYWORD)
        abstract Builder keyword(ValueReference keyword);

        abstract KeywordRangeEntity autoBuild();

        KeywordRangeEntity build() {
            type(ModelTypeEntity.of(TYPE));
            return autoBuild();
        }
    }
}

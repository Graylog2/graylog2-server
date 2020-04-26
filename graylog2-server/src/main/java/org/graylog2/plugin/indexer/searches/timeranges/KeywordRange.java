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
package org.graylog2.plugin.indexer.searches.timeranges;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import org.graylog2.plugin.utilities.date.NaturalDateParser;
import org.joda.time.DateTime;

import java.util.Map;

@AutoValue
@JsonTypeName(KeywordRange.KEYWORD)
public abstract class KeywordRange extends TimeRange {

    private static final NaturalDateParser DATE_PARSER = new NaturalDateParser();

    public static final String KEYWORD = "keyword";

    @JsonProperty
    @Override
    public abstract String type();

    @JsonProperty
    public abstract String keyword();

    @JsonProperty
    public abstract String timezone();

    private static NaturalDateParser.Result parseResult(String keyword, String timezone) throws InvalidRangeParametersException {
        try {
            return DATE_PARSER.parse(keyword, timezone);
        } catch (NaturalDateParser.DateNotParsableException e) {
            throw new InvalidRangeParametersException("Could not parse from natural date: " + keyword);
        }
    }

    @JsonCreator
    public static KeywordRange create(@JsonProperty("type") String type, @JsonProperty("keyword") String keyword, @JsonProperty("timezone") String timezone) throws InvalidRangeParametersException {
        return builder().type(type).keyword(keyword).timezone(timezone).build();
    }

    public static KeywordRange create(String keyword, String timezone) throws InvalidRangeParametersException {
        return create(KEYWORD, keyword, timezone);
    }

    private static Builder builder() {
        return new AutoValue_KeywordRange.Builder();
    }

    public String getKeyword() {
        return keyword();
    }

    public String getTimezone() {
        return timezone();
    }

    @JsonIgnore
    @Override
    public DateTime getFrom() {
        try {
            return parseResult(keyword(), timezone()).getFrom();
        } catch (InvalidRangeParametersException e) {
            return null;
        }
    }

    @JsonIgnore
    @Override
    public DateTime getTo() {
        try {
            return parseResult(keyword(), timezone()).getTo();
        } catch (InvalidRangeParametersException e) {
            return null;
        }
    }

    @Override
    public Map<String, Object> getPersistedConfig() {
        return ImmutableMap.<String, Object>builder()
                .put("type", KEYWORD)
                .put("keyword", getKeyword())
                .put("timezone", getTimezone())
                .build();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder type(String type);

        public abstract Builder keyword(String keyword);

        public abstract Builder timezone(String timezone);

        abstract String keyword();

        abstract String timezone();

        abstract KeywordRange autoBuild();

        public KeywordRange build() throws InvalidRangeParametersException {
            parseResult(keyword(), timezone());
            return autoBuild();
        }
    }
}


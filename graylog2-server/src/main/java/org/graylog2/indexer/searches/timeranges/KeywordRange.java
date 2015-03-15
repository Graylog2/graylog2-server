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
package org.graylog2.indexer.searches.timeranges;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;
import org.graylog2.utilities.date.NaturalDateParser;
import org.joda.time.DateTime;

import java.util.Map;
import java.util.Objects;

import static com.google.common.base.Strings.isNullOrEmpty;

public class KeywordRange implements TimeRange {
    private static final NaturalDateParser DATE_PARSER = new NaturalDateParser();

    private final String keyword;
    private final boolean dynamic;

    private DateTime from;
    private DateTime to;

    public KeywordRange(String keyword, boolean dynamic) throws InvalidRangeParametersException {
        if (isNullOrEmpty(keyword)) {
            throw new InvalidRangeParametersException();
        }

        try {
            NaturalDateParser.Result result = parseKeyword(keyword);
            from = result.getFrom();
            to = result.getTo();
        } catch (NaturalDateParser.DateNotParsableException e) {
            throw new InvalidRangeParametersException("Could not parse from natural date: " + keyword);
        }

        this.keyword = keyword;
        this.dynamic = dynamic;
    }

    public KeywordRange(String keyword) throws InvalidRangeParametersException {
        this(keyword, false);
    }

    private NaturalDateParser.Result parseKeyword(String keyword) throws NaturalDateParser.DateNotParsableException {
        return DATE_PARSER.parse(keyword);
    }

    @Override
    public Type getType() {
        return Type.KEYWORD;
    }

    @Override
    public Map<String, Object> getPersistedConfig() {
        return ImmutableMap.<String, Object>builder()
                .put("type", getType().toString().toLowerCase())
                .put("keyword", getKeyword())
                .build();
    }

    public String getKeyword() {
        return keyword;
    }

    public DateTime getFrom() {
        try {
            return dynamic ? parseKeyword(keyword).getFrom() : from;
        } catch (NaturalDateParser.DateNotParsableException e) {
            // This should never happen
            return from;
        }
    }

    public DateTime getTo() {
        try {
            return dynamic ? parseKeyword(keyword).getTo() : to;
        } catch (NaturalDateParser.DateNotParsableException e) {
            // This should never happen
            return to;
        }
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("keyword", getKeyword())
                .add("from", getFrom())
                .add("to", getTo())
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        KeywordRange that = (KeywordRange) o;
        return dynamic == that.dynamic && keyword.equals(that.keyword);

    }

    @Override
    public int hashCode() {
        return Objects.hash(keyword, dynamic);
    }
}


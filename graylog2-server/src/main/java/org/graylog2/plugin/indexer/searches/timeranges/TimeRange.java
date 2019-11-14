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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.joda.time.DateTime;

import java.util.Map;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type", visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(name = AbsoluteRange.ABSOLUTE, value = AbsoluteRange.class),
        @JsonSubTypes.Type(name = RelativeRange.RELATIVE, value = RelativeRange.class),
        @JsonSubTypes.Type(name = KeywordRange.KEYWORD, value = KeywordRange.class)
})
public abstract class TimeRange {

    @JsonProperty
    public abstract String type();

    @JsonIgnore
    public abstract DateTime getFrom();

    @JsonIgnore
    public abstract DateTime getTo();

    @JsonIgnore
    // TODO: remove this when pre 3.2 dashboarding is removed.
    public abstract Map<String, Object> getPersistedConfig();

}

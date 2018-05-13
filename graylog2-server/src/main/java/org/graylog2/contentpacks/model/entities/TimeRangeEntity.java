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

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.graylog2.contentpacks.model.Typed;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = Typed.FIELD_META_TYPE, visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(name = AbsoluteRange.TYPE, value = AbsoluteRange.class),
        @JsonSubTypes.Type(name = RelativeRange.TYPE, value = RelativeRange.class),
        @JsonSubTypes.Type(name = KeywordRange.TYPE, value = KeywordRange.class)
})
public abstract class TimeRangeEntity implements Typed {
    interface TimeRangeBuilder<SELF> extends Typed.TypeBuilder<SELF> {
    }

    public static TimeRangeEntity of(TimeRange timeRange) {
        if (timeRange instanceof org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange) {
            return AbsoluteRange.of((org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange) timeRange);
        } else if (timeRange instanceof org.graylog2.plugin.indexer.searches.timeranges.KeywordRange) {
            return KeywordRange.of((org.graylog2.plugin.indexer.searches.timeranges.KeywordRange) timeRange);
        } else if (timeRange instanceof org.graylog2.plugin.indexer.searches.timeranges.RelativeRange) {
            return RelativeRange.of((org.graylog2.plugin.indexer.searches.timeranges.RelativeRange) timeRange);
        } else {
            throw new IllegalArgumentException("Unknown time range type " + timeRange.getClass());
        }
    }
}

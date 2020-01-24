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
import com.fasterxml.jackson.databind.annotation.JsonTypeResolver;
import org.graylog2.contentpacks.jackson.ValueReferenceTypeResolverBuilder;
import org.graylog2.contentpacks.model.entities.references.ValueReference;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
import org.graylog2.plugin.indexer.searches.timeranges.InvalidRangeParametersException;
import org.graylog2.plugin.indexer.searches.timeranges.KeywordRange;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;

import java.util.Map;

@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, include = JsonTypeInfo.As.WRAPPER_OBJECT, property = TypedEntity.FIELD_META_TYPE, visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(name = AbsoluteRangeEntity.TYPE, value = AbsoluteRangeEntity.class),
        @JsonSubTypes.Type(name = RelativeRangeEntity.TYPE, value = RelativeRangeEntity.class),
        @JsonSubTypes.Type(name = KeywordRangeEntity.TYPE, value = KeywordRangeEntity.class)
})
@JsonTypeResolver(ValueReferenceTypeResolverBuilder.class)
public abstract class TimeRangeEntity implements TypedEntity {
    interface TimeRangeBuilder<SELF> extends TypedEntity.TypeBuilder<SELF> {
    }

    public static TimeRangeEntity of(TimeRange timeRange) {
        if (timeRange instanceof AbsoluteRange) {
            return AbsoluteRangeEntity.of((AbsoluteRange) timeRange);
        } else if (timeRange instanceof KeywordRange) {
            return KeywordRangeEntity.of((KeywordRange) timeRange);
        } else if (timeRange instanceof RelativeRange) {
            return RelativeRangeEntity.of((RelativeRange) timeRange);
        } else {
            throw new IllegalArgumentException("Unknown time range type " + timeRange.getClass());
        }
    }

    public abstract TimeRange convert(Map<String, ValueReference> parameters);
}

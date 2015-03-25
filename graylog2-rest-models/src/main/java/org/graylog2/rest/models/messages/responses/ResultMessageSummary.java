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
package org.graylog2.rest.models.messages.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.collect.Multimap;
import com.google.common.collect.Range;

import javax.annotation.Nullable;
import java.util.Map;

@AutoValue
@JsonAutoDetect
public abstract class ResultMessageSummary {
    @JsonProperty("highlight_ranges")
    @Nullable
    public abstract Multimap<String, Range<Integer>> highlightRanges();

    @JsonProperty
    public abstract Map<String, Object> message();

    @JsonProperty
    public abstract String index();

    @JsonCreator
    public static ResultMessageSummary create(@Nullable @JsonProperty("highlight_ranges") Multimap<String, Range<Integer>> highlightRanges,
                                              @JsonProperty("message") Map<String, Object> message,
                                              @JsonProperty("index") String index) {
        return new AutoValue_ResultMessageSummary(highlightRanges, message, index);
    }
}

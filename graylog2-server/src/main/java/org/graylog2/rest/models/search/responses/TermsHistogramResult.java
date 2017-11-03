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
package org.graylog2.rest.models.search.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;

import java.util.Map;
import java.util.Set;

@JsonAutoDetect
@AutoValue
@WithBeanGetter
public abstract class TermsHistogramResult {
    @JsonProperty("time")
    public abstract long time();

    @JsonProperty("interval")
    public abstract String interval();

    @JsonProperty("size")
    public abstract long size();

    @JsonProperty("buckets")
    public abstract Map<Long, TermsResult> buckets();

    @JsonProperty("terms")
    public abstract Set<String> terms();

    @JsonProperty("built_query")
    public abstract String builtQuery();

    @JsonProperty("queried_timerange")
    public abstract TimeRange queriedTimerange();

    public static TermsHistogramResult create(long time, String interval, long size, Map<Long, TermsResult> buckets, Set<String> terms, String builtQuery, TimeRange queriedTimerange) {
        return new AutoValue_TermsHistogramResult(time, interval, size, buckets, terms, builtQuery, queriedTimerange);
    }
}

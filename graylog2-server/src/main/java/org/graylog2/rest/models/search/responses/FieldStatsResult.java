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

@JsonAutoDetect
@AutoValue
@WithBeanGetter
public abstract class FieldStatsResult {
    @JsonProperty
    public abstract long time();

    @JsonProperty
    public abstract long count();

    @JsonProperty
    public abstract double sum();

    @JsonProperty
    public abstract double sumOfSquares();

    @JsonProperty
    public abstract double mean();

    @JsonProperty
    public abstract double min();

    @JsonProperty
    public abstract double max();

    @JsonProperty
    public abstract double variance();

    @JsonProperty
    public abstract double stdDeviation();

    @JsonProperty
    public abstract String builtQuery();

    @JsonProperty
    public abstract long cardinality();

    public static FieldStatsResult create(long time,
                                          long count,
                                          double sum,
                                          double sumOfSquares,
                                          double mean,
                                          double min,
                                          double max,
                                          double variance,
                                          double stdDeviation,
                                          String builtQuery,
                                          long cardinality) {
        return new AutoValue_FieldStatsResult(time, count, sum, sumOfSquares, mean, min, max,
                variance, stdDeviation, builtQuery, cardinality);
    }
}

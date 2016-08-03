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
package org.graylog2.shared.system.stats.os;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

import java.util.List;

@JsonAutoDetect
@AutoValue
public abstract class OsStats {
    public static final double[] EMPTY_LOAD = new double[0];

    @JsonProperty
    @SuppressWarnings("mutable")
    public abstract double[] loadAverage();

    @JsonProperty
    public abstract long uptime();

    @JsonProperty
    public abstract Processor processor();

    @JsonProperty
    public abstract Memory memory();

    @JsonProperty
    public abstract Swap swap();

    public static OsStats create(double[] loadAverage,
                                 long uptime,
                                 Processor processor,
                                 Memory memory,
                                 Swap swap) {
        return new AutoValue_OsStats(loadAverage, uptime, processor, memory, swap);
    }
}

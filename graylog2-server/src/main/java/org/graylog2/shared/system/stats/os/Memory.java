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
import org.graylog.autovalue.WithBeanGetter;

@JsonAutoDetect
@AutoValue
@WithBeanGetter
public abstract class Memory {
    @JsonProperty
    public abstract long total();

    @JsonProperty
    public abstract long free();

    @JsonProperty
    public abstract short freePercent();

    @JsonProperty
    public abstract long used();

    @JsonProperty
    public abstract short usedPercent();

    @JsonProperty
    public abstract long actualFree();

    @JsonProperty
    public abstract long actualUsed();

    public static Memory create(long total,
                                long free,
                                short freePercent,
                                long used,
                                short usedPercent,
                                long actualFree,
                                long actualUsed) {
        return new AutoValue_Memory(total, free, freePercent, used, usedPercent, actualFree, actualUsed);
    }
}

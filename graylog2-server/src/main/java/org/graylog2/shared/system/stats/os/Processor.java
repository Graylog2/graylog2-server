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
public abstract class Processor {
    @JsonProperty
    public abstract String model();

    @JsonProperty
    public abstract String vendor();

    @JsonProperty
    public abstract int mhz();

    @JsonProperty
    public abstract int totalCores();

    @JsonProperty
    public abstract int totalSockets();

    @JsonProperty
    public abstract int coresPerSocket();

    @JsonProperty
    public abstract long cacheSize();

    @JsonProperty
    public abstract short sys();

    @JsonProperty
    public abstract short user();

    @JsonProperty
    public abstract short idle();

    @JsonProperty
    public abstract short stolen();

    public static Processor create(String model,
                                   String vendor,
                                   int mhz,
                                   int totalCores,
                                   int totalSockets,
                                   int coresPerSocket,
                                   long cacheSize,
                                   short sys,
                                   short user,
                                   short idle,
                                   short stolen) {
        return new AutoValue_Processor(model, vendor, mhz, totalCores, totalSockets, coresPerSocket, cacheSize,
                sys, user, idle, stolen);
    }
}

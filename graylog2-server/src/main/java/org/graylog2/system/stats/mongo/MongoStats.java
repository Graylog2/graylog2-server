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
package org.graylog2.system.stats.mongo;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.net.HostAndPort;
import org.graylog.autovalue.WithBeanGetter;

import javax.annotation.Nullable;
import java.util.List;

@JsonAutoDetect
@AutoValue
@WithBeanGetter
public abstract class MongoStats {
    @JsonProperty
    public abstract List<HostAndPort> servers();

    @JsonProperty
    public abstract BuildInfo buildInfo();

    @JsonProperty
    @Nullable
    public abstract HostInfo hostInfo();

    @JsonProperty
    @Nullable
    public abstract ServerStatus serverStatus();

    @JsonProperty
    @Nullable
    public abstract DatabaseStats databaseStats();

    public static MongoStats create(List<HostAndPort> servers,
                                    BuildInfo buildInfo,
                                    @Nullable HostInfo hostInfo,
                                    @Nullable ServerStatus serverStatus,
                                    @Nullable DatabaseStats databaseStats) {
        return new AutoValue_MongoStats(servers, buildInfo, hostInfo, serverStatus, databaseStats);
    }
}

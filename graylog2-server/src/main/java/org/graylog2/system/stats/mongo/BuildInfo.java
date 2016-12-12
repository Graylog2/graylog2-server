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
import org.graylog.autovalue.WithBeanGetter;

import javax.annotation.Nullable;
import java.util.List;

/**
 * @see <a href=http://docs.mongodb.org/manual/reference/command/buildInfo/>Diagnostic Commands &gt; buildInfo</a>
 */
@JsonAutoDetect
@AutoValue
@WithBeanGetter
public abstract class BuildInfo {
    @JsonProperty
    public abstract String version();

    @JsonProperty
    public abstract String gitVersion();

    @JsonProperty
    public abstract String sysInfo();

    @JsonProperty
    @Nullable
    public abstract String loaderFlags();

    @JsonProperty
    @Nullable
    public abstract String compilerFlags();

    @JsonProperty
    @Nullable
    public abstract String allocator();

    @JsonProperty
    public abstract List<Integer> versionArray();

    @JsonProperty
    @Nullable
    public abstract String javascriptEngine();

    @JsonProperty
    public abstract int bits();

    @JsonProperty
    public abstract boolean debug();

    @JsonProperty
    public abstract long maxBsonObjectSize();

    public static BuildInfo create(String version,
                                   String gitVersion,
                                   String sysInfo,
                                   @Nullable String loaderFlags,
                                   @Nullable String compilerFlags,
                                   @Nullable String allocator,
                                   List<Integer> versionArray,
                                   @Nullable String javascriptEngine,
                                   int bits,
                                   boolean debug,
                                   long maxBsonObjectSize) {
        return new AutoValue_BuildInfo(version, gitVersion, sysInfo, loaderFlags, compilerFlags, allocator,
                versionArray, javascriptEngine, bits, debug, maxBsonObjectSize);
    }
}

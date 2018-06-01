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
package org.graylog.plugins.sidecar.rest.models;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.joda.time.DateTime;

@AutoValue
@JsonAutoDetect
public abstract class NodeLogFile {
    @JsonProperty("path")
    public abstract String path();

    @JsonProperty("mod_time")
    public abstract DateTime modTime();

    @JsonProperty("size")
    public abstract long size();

    @JsonProperty("is_dir")
    public abstract boolean isDir();

    @JsonCreator
    public static NodeLogFile create(@JsonProperty("path") String path,
                                     @JsonProperty("mod_time") DateTime modTime,
                                     @JsonProperty("size") long size,
                                     @JsonProperty("is_dir") boolean isDir) {
        return new AutoValue_NodeLogFile(path, modTime, size, isDir);
    }
}

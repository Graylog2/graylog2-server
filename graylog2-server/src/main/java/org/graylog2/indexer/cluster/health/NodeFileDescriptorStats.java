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
package org.graylog2.indexer.cluster.health;

import com.google.auto.value.AutoValue;

import javax.annotation.Nullable;
import java.util.Optional;

@AutoValue
public abstract class NodeFileDescriptorStats {
    public abstract String name();

    public abstract String ip();

    @Nullable
    public abstract String host();

    public abstract Optional<Long> fileDescriptorMax();

    public static NodeFileDescriptorStats create(String name, String ip, @Nullable String host, Long fileDescriptorMax) {
        return new AutoValue_NodeFileDescriptorStats(name, ip, host, Optional.ofNullable(fileDescriptorMax));
    }
}

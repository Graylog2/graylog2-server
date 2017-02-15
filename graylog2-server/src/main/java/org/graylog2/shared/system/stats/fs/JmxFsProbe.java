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
package org.graylog2.shared.system.stats.fs;

import com.google.common.collect.ImmutableSet;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class JmxFsProbe implements FsProbe {
    private final Set<File> locations;

    @Inject
    public JmxFsProbe(@Named("message_journal_dir") File journalDirectory) {
        this.locations = ImmutableSet.of(journalDirectory);
    }

    @Override
    public FsStats fsStats() {
        final Map<String, FsStats.Filesystem> filesystems = new HashMap<>(locations.size());

        for (File location : locations) {
            final String path = location.getAbsolutePath();
            final long total = location.getTotalSpace();
            final long free = location.getFreeSpace();
            final long available = location.getUsableSpace();
            final long used = total - free;
            final short usedPercent = (short) ((double) used / total * 100);

            final FsStats.Filesystem filesystem = FsStats.Filesystem.create(
                    path, total, free, available, used, usedPercent);

            filesystems.put(path, filesystem);
        }

        return FsStats.create(filesystems);
    }
}

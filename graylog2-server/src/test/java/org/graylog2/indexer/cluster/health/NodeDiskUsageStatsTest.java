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

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class NodeDiskUsageStatsTest {

    private NodeDiskUsageStats nodeDiskUsageStats;

    @Test
    public void createWithValidValues() {
        nodeDiskUsageStats = NodeDiskUsageStats.create(
                "name",
                "0.0.0.0",
                "myelasticnode.graylog.org",
                "1gb",
                "20Gb",
                30.5D
        );
        assertThat(nodeDiskUsageStats.name()).isEqualTo("name");
        assertThat(nodeDiskUsageStats.ip()).isEqualTo("0.0.0.0");
        assertThat(nodeDiskUsageStats.host()).isEqualTo("myelasticnode.graylog.org");
        assertThat(nodeDiskUsageStats.diskUsed().getBytes()).isEqualTo(1073741824L);
        assertThat(nodeDiskUsageStats.diskTotal().getBytes()).isEqualTo(21474836480L);
        assertThat(nodeDiskUsageStats.diskUsedPercent()).isEqualTo(30.5D);
    }

    @Test
    public void hostCanBeNull() {
        nodeDiskUsageStats = NodeDiskUsageStats.create(
                "name",
                "0.0.0.0",
                null,
                "1mb",
                "2mb",
                99D
        );
        assertThat(nodeDiskUsageStats.host()).isNull();
    }

    @Test
    public void diskAvailabileIsCorrect() {
        nodeDiskUsageStats = NodeDiskUsageStats.create(
                "name",
                "0.0.0.0",
                null,
                "1gb",
                "3Gb",
                33.3D
        );
        assertThat(nodeDiskUsageStats.diskAvailable().getBytes()).isEqualTo(2147483648L);
    }
}

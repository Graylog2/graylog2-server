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

import org.elasticsearch.common.unit.ByteSizeValue;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ClusterAllocationDiskSettingsFactoryTest {

    @Test
    public void createPercentageWatermarkSettings() throws Exception {
        ClusterAllocationDiskSettings settings = ClusterAllocationDiskSettingsFactory.create(true, "75%", "85%", "99%");

        assertThat(settings).isInstanceOf(ClusterAllocationDiskSettings.class);
        assertThat(settings.ThresholdEnabled()).isTrue();
        assertThat(settings.watermarkSettings()).isInstanceOf(PercentageWatermarkSettings.class);
        assertThat(settings.watermarkSettings().type()).isEqualTo(WatermarkSettings.SettingsType.PERCENTAGE);
        assertThat(settings.watermarkSettings().low()).isEqualTo(75D);
        assertThat(settings.watermarkSettings().high()).isEqualTo(85D);
        assertThat(settings.watermarkSettings().floodStage()).isEqualTo(99D);
    }

    @Test
    public void createPercentageWatermarkSettingsWithoutFloodStage() throws Exception {
        ClusterAllocationDiskSettings settings = ClusterAllocationDiskSettingsFactory.create(true, "65%", "75%", "");

        assertThat(settings).isInstanceOf(ClusterAllocationDiskSettings.class);
        assertThat(settings.ThresholdEnabled()).isTrue();
        assertThat(settings.watermarkSettings()).isInstanceOf(PercentageWatermarkSettings.class);
        assertThat(settings.watermarkSettings().type()).isEqualTo(WatermarkSettings.SettingsType.PERCENTAGE);
        assertThat(settings.watermarkSettings().low()).isEqualTo(65D);
        assertThat(settings.watermarkSettings().high()).isEqualTo(75D);
        assertThat(settings.watermarkSettings().floodStage()).isNull();
    }

    @Test
    public void createAbsoluteValueWatermarkSettings() throws Exception {
        ClusterAllocationDiskSettings clusterAllocationDiskSettings = ClusterAllocationDiskSettingsFactory.create(true, "20Gb", "10Gb", "5Gb");

        assertThat(clusterAllocationDiskSettings).isInstanceOf(ClusterAllocationDiskSettings.class);
        assertThat(clusterAllocationDiskSettings.ThresholdEnabled()).isTrue();
        assertThat(clusterAllocationDiskSettings.watermarkSettings()).isInstanceOf(AbsoluteValueWatermarkSettings.class);

        AbsoluteValueWatermarkSettings settings = (AbsoluteValueWatermarkSettings) clusterAllocationDiskSettings.watermarkSettings();

        assertThat(settings.type()).isEqualTo(WatermarkSettings.SettingsType.ABSOLUTE);
        assertThat(settings.low()).isInstanceOf(ByteSizeValue.class);
        assertThat(settings.low().getBytes()).isEqualTo(21474836480L);
        assertThat(settings.high()).isInstanceOf(ByteSizeValue.class);
        assertThat(settings.high().getBytes()).isEqualTo(10737418240L);
        assertThat(settings.floodStage()).isInstanceOf(ByteSizeValue.class);
        assertThat(settings.floodStage().getBytes()).isEqualTo(5368709120L);
    }

    @Test
    public void createAbsoluteValueWatermarkSettingsWithoutFloodStage() throws Exception {
        ClusterAllocationDiskSettings clusterAllocationDiskSettings = ClusterAllocationDiskSettingsFactory.create(true, "10Gb", "5Gb", "");

        assertThat(clusterAllocationDiskSettings).isInstanceOf(ClusterAllocationDiskSettings.class);
        assertThat(clusterAllocationDiskSettings.ThresholdEnabled()).isTrue();
        assertThat(clusterAllocationDiskSettings.watermarkSettings()).isInstanceOf(AbsoluteValueWatermarkSettings.class);

        AbsoluteValueWatermarkSettings settings = (AbsoluteValueWatermarkSettings) clusterAllocationDiskSettings.watermarkSettings();

        assertThat(settings.type()).isEqualTo(WatermarkSettings.SettingsType.ABSOLUTE);
        assertThat(settings.low()).isInstanceOf(ByteSizeValue.class);
        assertThat(settings.low().getBytes()).isEqualTo(10737418240L);
        assertThat(settings.high()).isInstanceOf(ByteSizeValue.class);
        assertThat(settings.high().getBytes()).isEqualTo(5368709120L);
        assertThat(settings.floodStage()).isNull();
    }

    @Test
    public void createWithoutSettingsWhenThresholdDisabled() throws Exception {
        ClusterAllocationDiskSettings settings = ClusterAllocationDiskSettingsFactory.create(false, "", "", "");

        assertThat(settings).isInstanceOf(ClusterAllocationDiskSettings.class);
        assertThat(settings.ThresholdEnabled()).isFalse();
    }

    @Test(expected = Exception.class)
    public void throwExceptionWhenMixedSettings() throws Exception {
        ClusterAllocationDiskSettingsFactory.create(true, "10Gb", "10%", "");
    }
}

/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog2.indexer.cluster.health;

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
        assertThat(settings.low()).isInstanceOf(ByteSize.class);
        assertThat(settings.low().getBytes()).isEqualTo(21474836480L);
        assertThat(settings.high()).isInstanceOf(ByteSize.class);
        assertThat(settings.high().getBytes()).isEqualTo(10737418240L);
        assertThat(settings.floodStage()).isInstanceOf(ByteSize.class);
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
        assertThat(settings.low()).isInstanceOf(ByteSize.class);
        assertThat(settings.low().getBytes()).isEqualTo(10737418240L);
        assertThat(settings.high()).isInstanceOf(ByteSize.class);
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

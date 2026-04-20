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
package org.graylog2.configuration;

import com.github.joschi.jadconfig.JadConfig;
import com.github.joschi.jadconfig.RepositoryException;
import com.github.joschi.jadconfig.ValidationException;
import com.github.joschi.jadconfig.repositories.InMemoryRepository;
import com.github.joschi.jadconfig.util.Duration;
import org.joda.time.Period;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ElasticsearchConfigurationTest {
    @Test
    void testVerifyDefaults() throws RepositoryException, ValidationException {
        ElasticsearchConfiguration configuration = new ElasticsearchConfiguration();
        createJadConfig(Map.of(), configuration).process();
        assertEquals("graylog", configuration.getDefaultIndexPrefix());
        assertEquals("graylog-internal", configuration.getDefaultIndexTemplateName());
        assertEquals("gl-events", configuration.getDefaultEventsIndexPrefix());
        assertEquals("gl-system-events", configuration.getDefaultSystemEventsIndexPrefix());
        assertEquals("standard", configuration.getAnalyzer());
        assertEquals(1, configuration.getShards());
        assertEquals(0, configuration.getReplicas());
        assertFalse(configuration.isDisableIndexOptimization());
        assertEquals(1, configuration.getIndexOptimizationMaxNumSegments());
        assertEquals(Duration.seconds(5), configuration.getIndexFieldTypeRefreshInterval());
        assertEquals(Duration.minutes(5), configuration.getIndexFieldTypePeriodicalFullRefreshInterval());
        assertEquals("delete", configuration.getRetentionStrategy());
        assertEquals("time-size-optimizing", configuration.getRotationStrategy());
        assertEquals(Period.days(1), configuration.getMaxTimePerIndex());
        assertFalse(configuration.isRotateEmptyIndex());
        assertEquals(20000000, configuration.getMaxDocsPerIndex());
        assertEquals(30L * 1024 * 1024 * 1024 /* 30GB */, configuration.getMaxSizePerIndex());
        assertEquals(20, configuration.getMaxNumberOfIndices());
        assertFalse(configuration.isDisableVersionCheck());
        assertNull(configuration.getMaxWriteIndexAge());
        assertTrue(configuration.performRetention());
        assertEquals(4, configuration.getEnabledRotationStrategies().size());
        assertNull(configuration.getMaxIndexRetentionPeriod());
        assertEquals(Duration.hours(1), configuration.getIndexOptimizationTimeout());
        assertEquals(10, configuration.getIndexOptimizationJobs());
    }

    @Test
    void jadConfigOnlyCheckShardSizeIfSet() throws ValidationException, RepositoryException {

        ElasticsearchConfiguration configWithNullValues = new ElasticsearchConfiguration();
        JadConfig jadConfig = createJadConfig(Map.of(), configWithNullValues);
        jadConfig.process();

        assertThat(configWithNullValues.getTimeSizeOptimizingRotationMaxShardSize()).isNull();
        assertThat(configWithNullValues.getTimeSizeOptimizingRotationMinShardSize()).isNull();

        jadConfig = createJadConfig(Map.of(
                "time_size_optimizing_rotation_max_shard_size", "1mb",
                "time_size_optimizing_rotation_min_shard_size", "2mb"
        ), new ElasticsearchConfiguration());
        assertThatExceptionOfType(ValidationException.class).isThrownBy(jadConfig::process)
                .withMessageContaining("cannot be larger than");
    }


    private JadConfig createJadConfig(Map<String, String> properties, ElasticsearchConfiguration configuration) {
        final InMemoryRepository repository = new InMemoryRepository(properties);
        return new JadConfig(repository, configuration);
    }

}

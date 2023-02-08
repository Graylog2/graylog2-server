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
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ElasticsearchConfigurationTest {
    @Test
    public void testVerifyDefaults() throws RepositoryException, ValidationException {
        ElasticsearchConfiguration configuration = new ElasticsearchConfiguration();
        new JadConfig(new InMemoryRepository(), configuration).process();
        assertEquals("graylog", configuration.getDefaultIndexPrefix());
        assertEquals("graylog-internal", configuration.getDefaultIndexTemplateName());
        assertEquals("gl-events", configuration.getDefaultEventsIndexPrefix());
        assertEquals("gl-system-events", configuration.getDefaultSystemEventsIndexPrefix());
        assertEquals("standard", configuration.getAnalyzer());
        assertEquals(1, configuration.getShards());
        assertEquals(0, configuration.getReplicas());
        assertFalse(configuration.isDisableIndexOptimization());
        assertEquals(1, configuration.getIndexOptimizationMaxNumSegments());
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
}

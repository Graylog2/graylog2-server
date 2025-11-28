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
package org.graylog2.indexer.rotation.strategies;

import org.graylog2.configuration.ElasticsearchConfiguration;
import org.graylog2.indexer.indexset.IndexSet;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.rotation.common.IndexRotator;
import org.graylog2.indexer.rotation.tso.TimeSizeOptimizingCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TimeBasedSizeOptimizingStrategyTest {

    private TimeBasedSizeOptimizingStrategy timeBasedSizeOptimizingStrategy;

    @Mock
    private IndexSet indexSet;
    @Mock
    private IndexSetConfig indexSetConfig;
    @Mock
    private ElasticsearchConfiguration elasticsearchConfiguration;
    @Mock
    private TimeSizeOptimizingCalculator calculator;
    @Mock
    private IndexRotator indexRotator;

    @BeforeEach
    void setUp() {
        timeBasedSizeOptimizingStrategy = new TimeBasedSizeOptimizingStrategy(
                elasticsearchConfiguration,
                indexRotator,
                calculator
        );

        when(indexSet.getConfig()).thenReturn(indexSetConfig);
    }

    @Test
    void testCorrectStrategy() {
        when(indexSetConfig.rotationStrategyConfig()).thenReturn(mock(MessageCountRotationStrategyConfig.class));

        assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> timeBasedSizeOptimizingStrategy.shouldRotate("any_index", indexSet))
                .withMessageContaining("Unsupported RotationStrategyConfig type");
    }

}

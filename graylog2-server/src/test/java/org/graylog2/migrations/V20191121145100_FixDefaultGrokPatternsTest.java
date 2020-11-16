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
package org.graylog2.migrations;

import org.graylog2.grok.GrokPattern;
import org.graylog2.grok.GrokPatternService;
import org.graylog2.migrations.V20191121145100_FixDefaultGrokPatterns.MigrationCompleted;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.database.ValidationException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;


public class V20191121145100_FixDefaultGrokPatternsTest {
    private static final String PATTERN_NAME = "COMMONAPACHELOG";

    @Mock
    private ClusterConfigService configService;
    @Mock
    private GrokPatternService grokPatternService;

    @InjectMocks
    private V20191121145100_FixDefaultGrokPatterns migration;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void alreadyMigrated() {
        final MigrationCompleted migrationCompleted = MigrationCompleted.create(Collections.singleton(PATTERN_NAME));
        when(configService.get(MigrationCompleted.class)).thenReturn(migrationCompleted);

        migration.upgrade();

        verifyZeroInteractions(grokPatternService);
    }

    @Test
    public void patternWasModified() throws ValidationException {
        final GrokPattern pattern = GrokPattern.builder()
                .name(PATTERN_NAME)
                .pattern("modified")
                .build();
        when(grokPatternService.loadByName(PATTERN_NAME)).thenReturn(Optional.of(pattern));
        migration.upgrade();
        verify(grokPatternService, never()).update(any(GrokPattern.class));
        verify(configService).write(MigrationCompleted.create(Collections.singleton(PATTERN_NAME)));
    }

    @Test
    public void upgrade() throws ValidationException {
        final V20191121145100_FixDefaultGrokPatterns.PatternToMigrate patternToMigrate =
                V20191121145100_FixDefaultGrokPatterns.patternsToMigrate.stream()
                        .filter(p -> PATTERN_NAME.equals(p.name()))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException(
                                "Test expects pattern with name " + PATTERN_NAME + " to " + "be migrated."));
        final GrokPattern pattern = GrokPattern.builder()
                .name(PATTERN_NAME)
                .pattern(patternToMigrate.migrateFrom())
                .build();
        when(grokPatternService.loadByName(PATTERN_NAME)).thenReturn(Optional.of(pattern));
        migration.upgrade();
        verify(grokPatternService).update(argThat(p -> PATTERN_NAME.equals(p.name()) && patternToMigrate.migrateTo()
                .equals(p.pattern())));
        verify(configService).write(MigrationCompleted.create(Collections.singleton(PATTERN_NAME)));
    }
}

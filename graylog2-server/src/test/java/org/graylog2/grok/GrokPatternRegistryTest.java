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
package org.graylog2.grok;

import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.krakens.grok.api.Grok;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
public class GrokPatternRegistryTest {
    private static final GrokPattern GROK_PATTERN = GrokPattern.create("TESTNUM", "[0-9]+");
    private static final Set<GrokPattern> GROK_PATTERNS = Collections.singleton(GROK_PATTERN);

    private GrokPatternRegistry grokPatternRegistry;
    private EventBus eventBus;
    @Mock
    private GrokPatternService grokPatternService;

    @BeforeEach
    public void setUp() {
        eventBus = new EventBus("Test");
        final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder().setNameFormat("updater-%d").build());
        when(grokPatternService.loadAll()).thenReturn(GROK_PATTERNS);
        grokPatternRegistry = new GrokPatternRegistry(eventBus, grokPatternService, executor);
    }

    @Test
    public void grokPatternsChanged() {
        final Set<GrokPattern> newPatterns = Collections.singleton(GrokPattern.create("NEW_PATTERN", "\\w+"));
        when(grokPatternService.loadAll()).thenReturn(newPatterns);
        eventBus.post(GrokPatternsUpdatedEvent.create(Collections.singleton("NEW_PATTERN")));

        assertThat(grokPatternRegistry.patterns()).isEqualTo(newPatterns);
    }

    @Test
    public void cachedGrokForPattern() {
        final Grok grok = grokPatternRegistry.cachedGrokForPattern("%{TESTNUM}");
        assertThat(grok.getPatterns()).containsEntry(GROK_PATTERN.name(), GROK_PATTERN.pattern());
    }

    @Test
    public void cachedGrokForPatternThrowsRuntimeException() {
        Throwable exception = assertThrows(RuntimeException.class, () -> {

            final Set<GrokPattern> newPatterns = Collections.singleton(GrokPattern.create("EMPTY", ""));
            when(grokPatternService.loadAll()).thenReturn(newPatterns);
            eventBus.post(GrokPatternsUpdatedEvent.create(Collections.singleton("EMPTY")));

            grokPatternRegistry.cachedGrokForPattern("%{EMPTY}");
        });
        org.hamcrest.MatcherAssert.assertThat(exception.getMessage(), containsString("No definition for key 'EMPTY' found, aborting"));
        assertThat(exception.getCause()).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void cachedGrokForPatternWithNamedCaptureOnly() {
        final Grok grok = grokPatternRegistry.cachedGrokForPattern("%{TESTNUM}", true);
        assertThat(grok.getPatterns()).containsEntry(GROK_PATTERN.name(), GROK_PATTERN.pattern());
    }

    @Test
    public void cachedGrokForPatternWithNamedCaptureOnlyThrowsRuntimeException() {
        Throwable exception = assertThrows(RuntimeException.class, () -> {

            final Set<GrokPattern> newPatterns = Collections.singleton(GrokPattern.create("EMPTY", ""));
            when(grokPatternService.loadAll()).thenReturn(newPatterns);
            eventBus.post(GrokPatternsUpdatedEvent.create(Collections.singleton("EMPTY")));

            grokPatternRegistry.cachedGrokForPattern("%{EMPTY}", true);
        });
        org.hamcrest.MatcherAssert.assertThat(exception.getMessage(), containsString("No definition for key 'EMPTY' found, aborting"));
        assertThat(exception.getCause()).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void patterns() {
        assertThat(grokPatternRegistry.patterns()).isEqualTo(GROK_PATTERNS);
    }

    @Test
    public void patternExists() {
        assertThat(grokPatternRegistry.grokPatternExists("TEST")).isFalse();
        assertThat(grokPatternRegistry.grokPatternExists("NUM")).isFalse();
        assertThat(grokPatternRegistry.grokPatternExists("TESTNUM")).isTrue();
    }
}

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
package org.graylog2.grok;

import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.thekraken.grok.api.Grok;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class GrokPatternRegistryTest {
    private static final GrokPattern GROK_PATTERN = GrokPattern.create("TESTNUM", "[0-9]+");
    private static final Set<GrokPattern> GROK_PATTERNS = Collections.singleton(GROK_PATTERN);

    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    private GrokPatternRegistry grokPatternRegistry;
    private EventBus eventBus;
    @Mock
    private GrokPatternService grokPatternService;

    @Before
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
        expectedException.expectMessage("No definition for key 'EMPTY' found, aborting");
        expectedException.expect(RuntimeException.class);
        expectedException.expectCause(Matchers.any(IllegalArgumentException.class));

        final Set<GrokPattern> newPatterns = Collections.singleton(GrokPattern.create("EMPTY", ""));
        when(grokPatternService.loadAll()).thenReturn(newPatterns);
        eventBus.post(GrokPatternsUpdatedEvent.create(Collections.singleton("EMPTY")));

        grokPatternRegistry.cachedGrokForPattern("%{EMPTY}");
    }

    @Test
    public void cachedGrokForPatternWithNamedCaptureOnly() {
        final Grok grok = grokPatternRegistry.cachedGrokForPattern("%{TESTNUM}", true);
        assertThat(grok.getPatterns()).containsEntry(GROK_PATTERN.name(), GROK_PATTERN.pattern());
    }

    @Test
    public void cachedGrokForPatternWithNamedCaptureOnlyThrowsRuntimeException() {
        expectedException.expectMessage("No definition for key 'EMPTY' found, aborting");
        expectedException.expect(RuntimeException.class);
        expectedException.expectCause(Matchers.any(IllegalArgumentException.class));

        final Set<GrokPattern> newPatterns = Collections.singleton(GrokPattern.create("EMPTY", ""));
        when(grokPatternService.loadAll()).thenReturn(newPatterns);
        eventBus.post(GrokPatternsUpdatedEvent.create(Collections.singleton("EMPTY")));

        grokPatternRegistry.cachedGrokForPattern("%{EMPTY}", true);
    }

    @Test
    public void patterns() {
        assertThat(grokPatternRegistry.patterns()).isEqualTo(GROK_PATTERNS);
    }
}
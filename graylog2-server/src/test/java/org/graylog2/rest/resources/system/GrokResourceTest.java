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
package org.graylog2.rest.resources.system;

import com.google.common.eventbus.Subscribe;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.grok.GrokPattern;
import org.graylog2.grok.GrokPatternService;
import org.graylog2.grok.GrokPatternsChangedEvent;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.shared.bindings.GuiceInjectorHolder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GrokResourceTest {
    private static final String[] GROK_LINES = {
            "# Comment",
            "",
            "TEST_PATTERN_0 Foo"
    };

    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    @Mock
    private GrokPatternService grokPatternService;

    private GrokResource grokResource;
    private GrokPatternsChangedEventSubscriber subscriber;

    public GrokResourceTest() {
        GuiceInjectorHolder.createInjector(Collections.emptyList());
    }

    @Before
    public void setUp() {
        final ClusterEventBus clusterBus = new ClusterEventBus();
        subscriber = new GrokPatternsChangedEventSubscriber();
        clusterBus.registerClusterEventSubscriber(subscriber);
        grokResource = new PermittedTestResource(grokPatternService, clusterBus);
    }

    @Test
    public void bulkUpdatePatternsFromTextFileWithLF() throws Exception {
        final String patterns = Arrays.stream(GROK_LINES).collect(Collectors.joining("\n"));
        final ByteArrayInputStream inputStream = new ByteArrayInputStream(patterns.getBytes(StandardCharsets.UTF_8));
        final GrokPattern expectedPattern = GrokPattern.create("TEST_PATTERN_0", "Foo");
        when(grokPatternService.validate(expectedPattern)).thenReturn(true);

        final Response response = grokResource.bulkUpdatePatternsFromTextFile(inputStream, true);

        verify(grokPatternService, times(1)).validate(expectedPattern);
        verify(grokPatternService, times(1)).saveAll(Collections.singletonList(expectedPattern), true);

        assertThat(response.getStatusInfo()).isEqualTo(Response.Status.ACCEPTED);
        assertThat(response.hasEntity()).isFalse();
        assertThat(subscriber.events)
                .containsOnly(GrokPatternsChangedEvent.create(Collections.emptySet(), Collections.singleton(expectedPattern.name())));
    }

    @Test
    public void bulkUpdatePatternsFromTextFileWithCR() throws Exception {
        final String patterns = Arrays.stream(GROK_LINES).collect(Collectors.joining("\r"));
        final ByteArrayInputStream inputStream = new ByteArrayInputStream(patterns.getBytes(StandardCharsets.UTF_8));
        final GrokPattern expectedPattern = GrokPattern.create("TEST_PATTERN_0", "Foo");
        when(grokPatternService.validate(expectedPattern)).thenReturn(true);

        final Response response = grokResource.bulkUpdatePatternsFromTextFile(inputStream, true);

        verify(grokPatternService, times(1)).validate(expectedPattern);
        verify(grokPatternService, times(1)).saveAll(Collections.singletonList(expectedPattern), true);

        assertThat(response.getStatusInfo()).isEqualTo(Response.Status.ACCEPTED);
        assertThat(response.hasEntity()).isFalse();
        assertThat(subscriber.events)
                .containsOnly(GrokPatternsChangedEvent.create(Collections.emptySet(), Collections.singleton(expectedPattern.name())));
    }

    @Test
    public void bulkUpdatePatternsFromTextFileWithCRLF() throws Exception {
        final String patterns = Arrays.stream(GROK_LINES).collect(Collectors.joining("\r\n"));
        final ByteArrayInputStream inputStream = new ByteArrayInputStream(patterns.getBytes(StandardCharsets.UTF_8));
        final GrokPattern expectedPattern = GrokPattern.create("TEST_PATTERN_0", "Foo");
        when(grokPatternService.validate(expectedPattern)).thenReturn(true);

        final Response response = grokResource.bulkUpdatePatternsFromTextFile(inputStream, true);

        verify(grokPatternService, times(1)).validate(expectedPattern);
        verify(grokPatternService, times(1)).saveAll(Collections.singletonList(expectedPattern), true);

        assertThat(response.getStatusInfo()).isEqualTo(Response.Status.ACCEPTED);
        assertThat(response.hasEntity()).isFalse();
        assertThat(subscriber.events)
                .containsOnly(GrokPatternsChangedEvent.create(Collections.emptySet(), Collections.singleton(expectedPattern.name())));
    }

    @Test
    public void bulkUpdatePatternsFromTextFileWithInvalidPattern() throws Exception {
        final String patterns = Arrays.stream(GROK_LINES).collect(Collectors.joining("\n"));
        final ByteArrayInputStream inputStream = new ByteArrayInputStream(patterns.getBytes(StandardCharsets.UTF_8));
        final GrokPattern expectedPattern = GrokPattern.create("TEST_PATTERN_0", "Foo");
        when(grokPatternService.validate(expectedPattern)).thenReturn(false);

        expectedException.expect(ValidationException.class);
        expectedException.expectMessage("Invalid pattern " + expectedPattern + ". Did not save any patterns.");

        grokResource.bulkUpdatePatternsFromTextFile(inputStream, true);
    }

    @Test
    public void bulkUpdatePatternsFromTextFileWithNoValidPatterns() throws Exception {
        final String patterns = "# Comment\nHAHAHAHA_THIS_IS_NO_GROK_PATTERN!$%§";
        final ByteArrayInputStream inputStream = new ByteArrayInputStream(patterns.getBytes(StandardCharsets.UTF_8));

        final Response response = grokResource.bulkUpdatePatternsFromTextFile(inputStream, true);

        verify(grokPatternService, never()).validate(any(GrokPattern.class));
        verify(grokPatternService, never()).saveAll(any(), eq(true));

        assertThat(response.getStatusInfo()).isEqualTo(Response.Status.ACCEPTED);
        assertThat(response.hasEntity()).isFalse();
        assertThat(subscriber.events).isEmpty();
    }

    static class GrokPatternsChangedEventSubscriber {
        final List<GrokPatternsChangedEvent> events = new ArrayList<>();

        @Subscribe
        public void handleEvent(GrokPatternsChangedEvent event) {
            events.add(event);
        }
    }

    static class PermittedTestResource extends GrokResource {
        PermittedTestResource(GrokPatternService grokPatternService, ClusterEventBus clusterBus) {
            super(grokPatternService, clusterBus);
        }

        @Override
        protected boolean isPermitted(String permission) {
            return true;
        }
    }
}
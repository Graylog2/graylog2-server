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
import org.graylog2.grok.GrokPatternsDeletedEvent;
import org.graylog2.grok.GrokPatternsUpdatedEvent;
import org.graylog2.grok.InMemoryGrokPatternService;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.rest.models.system.grokpattern.requests.GrokPatternTestRequest;
import org.graylog2.shared.SuppressForbidden;
import org.graylog2.shared.bindings.GuiceInjectorHolder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

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

    private InMemoryGrokPatternService grokPatternService;

    private GrokResource grokResource;
    private GrokPatternsChangedEventSubscriber subscriber;

    public GrokResourceTest() {
        GuiceInjectorHolder.createInjector(Collections.emptyList());
    }

    @Before
    @SuppressForbidden("Using Executors.newSingleThreadExecutor() is okay in tests")
    public void setUp() {
        final ClusterEventBus clusterBus = new ClusterEventBus("cluster-event-bus", Executors.newSingleThreadExecutor());
        grokPatternService = new InMemoryGrokPatternService(clusterBus);
        subscriber = new GrokPatternsChangedEventSubscriber();
        clusterBus.registerClusterEventSubscriber(subscriber);
        grokResource = new PermittedTestResource(grokPatternService);
    }

    @Test
    public void bulkUpdatePatternsFromTextFileWithLF() throws Exception {
        final String patterns = Arrays.stream(GROK_LINES).collect(Collectors.joining("\n"));
        final ByteArrayInputStream inputStream = new ByteArrayInputStream(patterns.getBytes(StandardCharsets.UTF_8));
        final GrokPattern expectedPattern = GrokPattern.create("TEST_PATTERN_0", "Foo");

        final Response response = grokResource.bulkUpdatePatternsFromTextFile(inputStream, true);

        assertThat(response.getStatusInfo()).isEqualTo(Response.Status.ACCEPTED);
        assertThat(response.hasEntity()).isFalse();
        assertThat(subscriber.events)
                .containsOnly(GrokPatternsUpdatedEvent.create(Collections.singleton(expectedPattern.name())));
    }

    @Test
    public void bulkUpdatePatternsFromTextFileWithCR() throws Exception {
        final String patterns = Arrays.stream(GROK_LINES).collect(Collectors.joining("\r"));
        final ByteArrayInputStream inputStream = new ByteArrayInputStream(patterns.getBytes(StandardCharsets.UTF_8));
        final GrokPattern expectedPattern = GrokPattern.create("TEST_PATTERN_0", "Foo");

        final Response response = grokResource.bulkUpdatePatternsFromTextFile(inputStream, true);

        assertThat(response.getStatusInfo()).isEqualTo(Response.Status.ACCEPTED);
        assertThat(response.hasEntity()).isFalse();
        assertThat(subscriber.events)
                .containsOnly(GrokPatternsUpdatedEvent.create(Collections.singleton(expectedPattern.name())));
    }

    @Test
    public void bulkUpdatePatternsFromTextFileWithCRLF() throws Exception {
        final String patterns = Arrays.stream(GROK_LINES).collect(Collectors.joining("\r\n"));
        final ByteArrayInputStream inputStream = new ByteArrayInputStream(patterns.getBytes(StandardCharsets.UTF_8));
        final GrokPattern expectedPattern = GrokPattern.create("TEST_PATTERN_0", "Foo");

        final Response response = grokResource.bulkUpdatePatternsFromTextFile(inputStream, true);

        assertThat(response.getStatusInfo()).isEqualTo(Response.Status.ACCEPTED);
        assertThat(response.hasEntity()).isFalse();
        assertThat(subscriber.events)
                .containsOnly(GrokPatternsUpdatedEvent.create(Collections.singleton(expectedPattern.name())));
    }

    @Test
    public void bulkUpdatePatternsFromTextFileWithInvalidPattern() throws Exception {
        final String patterns = "TEST_PATTERN_0 %{Foo";
        final ByteArrayInputStream inputStream = new ByteArrayInputStream(patterns.getBytes(StandardCharsets.UTF_8));

        expectedException.expect(ValidationException.class);
        expectedException.expectMessage("Invalid pattern. Did not save any patterns");

        grokResource.bulkUpdatePatternsFromTextFile(inputStream, true);
    }

    @Test
    public void bulkUpdatePatternsFromTextFileWithNoValidPatterns() throws Exception {
        final String patterns = "# Comment\nHAHAHAHA_THIS_IS_NO_GROK_PATTERN!$%§";
        final ByteArrayInputStream inputStream = new ByteArrayInputStream(patterns.getBytes(StandardCharsets.UTF_8));

        final Response response = grokResource.bulkUpdatePatternsFromTextFile(inputStream, true);

        assertThat(response.getStatusInfo()).isEqualTo(Response.Status.ACCEPTED);
        assertThat(response.hasEntity()).isFalse();
        assertThat(subscriber.events).isEmpty();
    }

    @Test
    public void testPatternWithSampleData() throws Exception {
        final String sampleData = "1.2.3.4";
        final GrokPattern grokPattern = GrokPattern.create("IP", "\\d.\\d.\\d.\\d");
        grokPatternService.save(grokPattern);
        final GrokPatternTestRequest grokPatternTestRequest = GrokPatternTestRequest.create(grokPattern, sampleData);
        final Map<String, Object> expectedReturn = Collections.singletonMap("IP", "1.2.3.4");

        // when(grokPatternService.match(grokPattern, sampleData)).thenReturn(expectedReturn);
        final Response response = grokResource.testPattern(grokPatternTestRequest);
        assertThat(response.hasEntity()).isTrue();
        assertThat(response.getEntity()).isEqualTo(expectedReturn);
    }

    static class GrokPatternsChangedEventSubscriber {
        final List<Object> events = new ArrayList<>();

        @Subscribe
        public void handleUpdatedEvent(GrokPatternsUpdatedEvent event) {
            events.add(event);
        }

        @Subscribe
        public void handleDeletedEvent(GrokPatternsDeletedEvent event) {
            events.add(event);
        }
    }

    static class PermittedTestResource extends GrokResource {
        PermittedTestResource(GrokPatternService grokPatternService) {
            super(grokPatternService);
        }

        @Override
        protected boolean isPermitted(String permission) {
            return true;
        }
    }
}
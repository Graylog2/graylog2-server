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
package org.graylog2.rest.resources.system;

import com.google.common.eventbus.Subscribe;
import org.awaitility.Duration;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.grok.GrokPattern;
import org.graylog2.grok.GrokPatternService;
import org.graylog2.grok.GrokPatternsDeletedEvent;
import org.graylog2.grok.GrokPatternsUpdatedEvent;
import org.graylog2.grok.InMemoryGrokPatternService;
import org.graylog2.grok.PaginatedGrokPatternService;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.rest.models.system.grokpattern.requests.GrokPatternTestRequest;
import org.graylog2.shared.SuppressForbidden;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.mock;

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
    private PaginatedGrokPatternService paginatedGrokPatternService;

    private InMemoryGrokPatternService grokPatternService;

    private GrokResource grokResource;
    private GrokPatternsChangedEventSubscriber subscriber;

    public GrokResourceTest() {
        GuiceInjectorHolder.createInjector(Collections.emptyList());
    }

    @Before
    @SuppressForbidden("Using Executors.newSingleThreadExecutor() is okay in tests")
    public void setUp() {
        paginatedGrokPatternService = mock(PaginatedGrokPatternService.class);
        final ClusterEventBus clusterBus = new ClusterEventBus("cluster-event-bus", Executors.newSingleThreadExecutor());
        grokPatternService = new InMemoryGrokPatternService(clusterBus);
        subscriber = new GrokPatternsChangedEventSubscriber();
        clusterBus.registerClusterEventSubscriber(subscriber);
        grokResource = new PermittedTestResource(grokPatternService, paginatedGrokPatternService);
    }

    @Test
    public void bulkUpdatePatternsFromTextFileWithLF() throws Exception {
        final String patterns = Arrays.stream(GROK_LINES).collect(Collectors.joining("\n"));
        final ByteArrayInputStream inputStream = new ByteArrayInputStream(patterns.getBytes(StandardCharsets.UTF_8));
        final GrokPattern expectedPattern = GrokPattern.create("TEST_PATTERN_0", "Foo");

        final Response response = grokResource.bulkUpdatePatternsFromTextFile(inputStream, true);

        assertThat(response.getStatusInfo()).isEqualTo(Response.Status.ACCEPTED);
        assertThat(response.hasEntity()).isFalse();

        await()
                .atMost(Duration.FIVE_SECONDS)
                .until(() -> !subscriber.events.isEmpty());
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

        await()
                .atMost(Duration.FIVE_SECONDS)
                .until(() -> !subscriber.events.isEmpty());
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

        await()
                .atMost(Duration.FIVE_SECONDS)
                .until(() -> !subscriber.events.isEmpty());
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

        final Response response = grokResource.testPattern(grokPatternTestRequest);
        assertThat(response.hasEntity()).isTrue();
        assertThat(response.getEntity()).isEqualTo(expectedReturn);
    }

    static class GrokPatternsChangedEventSubscriber {
        final List<Object> events = new CopyOnWriteArrayList<>();

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
        PermittedTestResource(GrokPatternService grokPatternService,
                              PaginatedGrokPatternService paginatedGrokPatternService) {
            super(grokPatternService, paginatedGrokPatternService);
        }

        @Override
        protected boolean isPermitted(String permission) {
            return true;
        }
    }
}
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
package org.graylog2.decorators;

import com.floreysoft.jmte.Engine;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import org.graylog2.plugin.MessageFactory;
import org.graylog2.plugin.TestMessageFactory;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.decorators.SearchResponseDecorator;
import org.graylog2.rest.models.messages.responses.ResultMessageSummary;
import org.graylog2.rest.models.system.indexer.responses.IndexRangeSummary;
import org.graylog2.rest.resources.search.responses.SearchResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class LinkFieldDecoratorTest {
    private final MessageFactory messageFactory = new TestMessageFactory();
    private final Engine templateEngine = new Engine();

    private static final String TEST_FIELD = "test_field";
    private LinkFieldDecorator decorator;

    @BeforeEach
    public void setUp() throws Exception {
        decorator = createDecorator(null);
    }

    private LinkFieldDecorator createDecorator(String linkText) {
        return createDecorator(null, linkText);
    }

    private LinkFieldDecorator createDecorator(String linkUrl, String linkText) {
        final HashMap<String, Object> config = new HashMap<>();
        config.put(LinkFieldDecorator.CK_LINK_FIELD, TEST_FIELD);
        if (linkUrl != null) {
            config.put(LinkFieldDecorator.CK_LINK_URL, linkUrl);
        }
        if (linkText != null) {
            config.put(LinkFieldDecorator.CK_LINK_TEXT, linkText);
        }
        return new LinkFieldDecorator(DecoratorImpl.create("id", "link", config, Optional.empty(), 0), templateEngine, messageFactory);
    }

    @Test
    public void omitsLinkTextWhenNotConfigured() {
        assertThat(getDecoratedField("https://example.com"))
                .containsEntry("href", "https://example.com")
                .doesNotContainKey("text");
    }

    @Test
    public void omitsLinkTextWhenBlank() {
        decorator = createDecorator("  ");

        assertThat(getDecoratedField("https://example.com")).doesNotContainKey("text");
    }

    @Test
    public void addsStaticLinkText() {
        decorator = createDecorator("Open in IPAM");

        assertThat(getDecoratedField("https://ipam.example.com/search?ip=10.0.0.1"))
                .containsEntry("href", "https://ipam.example.com/search?ip=10.0.0.1")
                .containsEntry("text", "Open in IPAM");
    }

    @Test
    public void addsLinkTextBuiltFromMessageFields() {
        decorator = createDecorator("Ticket ${ticket_id}");

        assertThat(getDecoratedField("https://tickets.example.com/42", Map.of("ticket_id", "42")))
                .containsEntry("text", "Ticket 42");
    }

    @Test
    public void omitsLinkTextWhenTemplateRendersBlank() {
        decorator = createDecorator("${ticket_id}");

        assertThat(getDecoratedField("https://tickets.example.com/42")).doesNotContainKey("text");
    }

    @Test
    public void doesNotUseLinkTextForUnsafeUrls() {
        decorator = createDecorator("Click me");

        assertThat(getDecoratorMessage("javascript:alert(1)")).isEqualTo("javascript:alert(1)");
    }

    @Test
    public void verifyUnsafeLinksAreRemoved() {
        // Verify that real, safe URLs are rendered as links.
        Assertions.assertEquals("http://full-local-allowed", getDecoratorUrl("http://full-local-allowed"));
        Assertions.assertEquals("http://full-url-allowed.com", getDecoratorUrl("http://full-url-allowed.com"));
        Assertions.assertEquals("http://full-url-allowed.com/test", getDecoratorUrl("http://full-url-allowed.com/test"));
        Assertions.assertEquals("http://full-url-allowed.com/test?with=param", getDecoratorUrl("http://full-url-allowed.com/test?with=param"));
        Assertions.assertEquals("https://https-is-allowed-too.com", getDecoratorUrl("https://https-is-allowed-too.com"));
        Assertions.assertEquals("HTTPS://upper-case-https-all-good.com", getDecoratorUrl("HTTPS://upper-case-https-all-good.com"));
        Assertions.assertEquals("HTTP://upper-case-https-all-good.com", getDecoratorUrl("HTTP://upper-case-https-all-good.com"));
        Assertions.assertEquals("https://nedlog.local:9000/search?q=event_source_product%3Alinux", getDecoratorUrl("https://nedlog.local:9000/search?q=event_source_product%3Alinux"));

        // Links with double slashes should be allowed.
        Assertions.assertEquals("https://graylog.com//releases", getDecoratorUrl("https://graylog.com//releases"));

        // Verify that unsafe URLs are rendered as text.
        Assertions.assertEquals("javascript:alert('Javascript is not allowed.')", getDecoratorMessage("javascript:alert('Javascript is not allowed.')"));
        Assertions.assertEquals("alert('Javascript this way is still not allowed", getDecoratorMessage("alert('Javascript this way is still not allowed"));
        Assertions.assertEquals("ntp://other-stuff-is-not-allowed", getDecoratorMessage("ntp://other-stuff-is-not-allowed"));
        Assertions.assertEquals("ftp://ftp-not-allowed", getDecoratorMessage("ftp://ftp-not-allowed"));
        Assertions.assertEquals("HTTP:", getDecoratorMessage("HTTP:"));
        Assertions.assertEquals("HTTP", getDecoratorMessage("HTTP"));
        Assertions.assertEquals("HTTPS:", getDecoratorMessage("HTTPS:"));
        Assertions.assertEquals("HTTPS", getDecoratorMessage("HTTPS"));
    }

    /**
     * @return Dig out and return the message value directly displayed by the UI.
     */
    private Object getDecoratorMessage(String urlFieldValue) {

        return executeDecoratorGetFirstMessage(urlFieldValue).message().get(TEST_FIELD);
    }

    /**
     * @return Dig out and return the href link property used by the UI.
     */
    private Object getDecoratorUrl(String urlFieldValue) {

        return ((HashMap) executeDecoratorGetFirstMessage(urlFieldValue).message().get(TEST_FIELD)).get("href");
    }

    @Test
    public void buildsUrlFromTemplate() {
        decorator = createDecorator("https://ipam.example.com/search?ip=${src_ip}", "Open in IPAM");

        assertThat(getDecoratedField("original value", Map.of("src_ip", "10.0.0.1")))
                .containsEntry("type", "a")
                .containsEntry("href", "https://ipam.example.com/search?ip=10.0.0.1")
                .containsEntry("text", "Open in IPAM");
    }

    @Test
    public void doesNotCreateLinkFieldFromTemplateIfMissing() {
        decorator = createDecorator("https://ipam.example.com/search?ip=${src_ip}", null);

        assertThat(getDecoratedMessage(null, Map.of("src_ip", "10.0.0.1"))).doesNotContainKey(TEST_FIELD);
    }

    @Test
    public void urlEncodesFieldValuesInTemplate() {
        decorator = createDecorator("https://example.com/search?q=${query}", null);

        assertThat(getDecoratedField("original value", Map.of("query", "a b&c=d+e/ä")))
                .containsEntry("href", "https://example.com/search?q=a%20b%26c%3Dd%2Be%2F%C3%A4");
    }

    @Test
    public void leavesMessageUnchangedIfTemplateFieldIsMissing() {
        decorator = createDecorator("https://ipam.example.com/search?ip=${src_ip}", null);

        assertThat(getDecoratedMessage("original value", Map.of())).containsEntry(TEST_FIELD, "original value");
    }

    @Test
    public void ignoresTemplateWithUnsafeScheme() {
        decorator = createDecorator("javascript:alert('${src_ip}')", null);

        assertThat(getDecoratedMessage("original value", Map.of("src_ip", "10.0.0.1")))
                .containsEntry(TEST_FIELD, "original value");
    }

    @Test
    public void leavesNonStringLinkFieldUnchanged() {
        final SearchResponse response = createSearchResponse(Map.of(TEST_FIELD, 42L));

        assertThat(decorator.apply(response).messages().get(0).message()).containsEntry(TEST_FIELD, 42L);
    }

    @Test
    public void leavesNullLinkFieldUnchanged() {
        final Map<String, Object> fields = new HashMap<>();
        fields.put(TEST_FIELD, null);

        final Map<String, Object> message = decorator.apply(createSearchResponse(fields)).messages().get(0).message();

        assertThat(message).containsEntry(TEST_FIELD, null);
    }

    @Test
    public void nonStringLinkFieldDoesNotPreventOtherDecorators() {
        final SearchResponseDecorator addMarker = response -> response.toBuilder()
                .messages(response.messages().stream()
                        .map(summary -> {
                            final Map<String, Object> fields = new HashMap<>(summary.message());
                            fields.put("marker", "applied");
                            return summary.toBuilder().message(fields).build();
                        })
                        .toList())
                .build();
        final DecoratorProcessorImpl processor = new DecoratorProcessorImpl(mock(DecoratorResolver.class));

        final SearchResponse result = processor.decorate(createSearchResponse(Map.of(TEST_FIELD, 42L)), List.of(decorator, addMarker));

        assertThat(result.messages().get(0).message())
                .containsEntry(TEST_FIELD, 42L)
                .containsEntry("marker", "applied");
    }

    private Map<String, String> getDecoratedField(String urlFieldValue) {
        return getDecoratedField(urlFieldValue, Map.of());
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> getDecoratedField(String urlFieldValue, Map<String, Object> additionalFields) {
        return (Map<String, String>) getDecoratedMessage(urlFieldValue, additionalFields).get(TEST_FIELD);
    }

    private Map<String, Object> getDecoratedMessage(String urlFieldValue, Map<String, Object> additionalFields) {
        return decorator.apply(createSearchResponse(urlFieldValue, additionalFields)).messages().get(0).message();
    }

    private ResultMessageSummary executeDecoratorGetFirstMessage(String urlFieldValue) {
        return decorator.apply(createSearchResponse(urlFieldValue, Map.of())).messages().get(0);
    }

    private SearchResponse createSearchResponse(String urlFieldValue, Map<String, Object> additionalFields) {
        final Map<String, Object> message = new HashMap<>(additionalFields);
        if (urlFieldValue != null) {
            message.put(TEST_FIELD, urlFieldValue);
        }
        return createSearchResponse(message);
    }

    private SearchResponse createSearchResponse(Map<String, Object> fields) {
        final Map<String, Object> message = new HashMap<>(fields);
        message.put("_id", "a");

        final List<ResultMessageSummary> messages = ImmutableList.of(
                ResultMessageSummary.create(ImmutableMultimap.of(), message, "graylog_0")
        );

        final IndexRangeSummary indexRangeSummary = IndexRangeSummary.create("graylog_0",
                                                                             Tools.nowUTC().minusDays(1),
                                                                             Tools.nowUTC(),
                                                                             null,
                                                                             100);

        return SearchResponse.builder()
                             .query("foo")
                             .builtQuery("foo")
                             .usedIndices(ImmutableSet.of(indexRangeSummary))
                             .messages(messages)
                             .fields(ImmutableSet.of(TEST_FIELD))
                             .time(100L)
                             .totalResults(messages.size())
                             .from(Tools.nowUTC().minusHours(1))
                             .to(Tools.nowUTC())
                             .build();
    }
}

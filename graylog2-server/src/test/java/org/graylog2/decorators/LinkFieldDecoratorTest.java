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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import org.graylog2.plugin.MessageFactory;
import org.graylog2.plugin.TestMessageFactory;
import org.graylog2.plugin.Tools;
import org.graylog2.rest.models.messages.responses.ResultMessageSummary;
import org.graylog2.rest.models.system.indexer.responses.IndexRangeSummary;
import org.graylog2.rest.resources.search.responses.SearchResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;

public class LinkFieldDecoratorTest {
    private final MessageFactory messageFactory = new TestMessageFactory();

    private static final String TEST_FIELD = "test_field";
    private LinkFieldDecorator decorator;

    @BeforeEach
    public void setUp() throws Exception {
        final HashMap<String, Object> config = new HashMap<>();
        config.put(LinkFieldDecorator.CK_LINK_FIELD, TEST_FIELD);
        decorator = new LinkFieldDecorator(DecoratorImpl.create("id", "link", config, Optional.empty(), 0), messageFactory);
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

    private ResultMessageSummary executeDecoratorGetFirstMessage(String urlFieldValue) {
        return decorator.apply(createSearchResponse(urlFieldValue)).messages().get(0);
    }

    private SearchResponse createSearchResponse(String urlFieldValue) {

        final List<ResultMessageSummary> messages = ImmutableList.of(
                ResultMessageSummary.create(ImmutableMultimap.of(), ImmutableMap.of("_id", "a", TEST_FIELD, urlFieldValue), "graylog_0")
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

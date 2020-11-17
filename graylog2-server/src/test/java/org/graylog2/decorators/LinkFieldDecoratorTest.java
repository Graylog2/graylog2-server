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
import org.graylog2.plugin.Tools;
import org.graylog2.rest.models.messages.responses.ResultMessageSummary;
import org.graylog2.rest.models.system.indexer.responses.IndexRangeSummary;
import org.graylog2.rest.resources.search.responses.SearchResponse;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;

public class LinkFieldDecoratorTest {

    private static final String TEST_FIELD = "test_field";
    private LinkFieldDecorator decorator;

    @Before
    public void setUp() throws Exception {
        final HashMap<String, Object> config = new HashMap<>();
        config.put(LinkFieldDecorator.CK_LINK_FIELD, TEST_FIELD);
        decorator = new LinkFieldDecorator(DecoratorImpl.create("id", "link", config, Optional.empty(), 0));
    }

    @Test
    public void verifyUnsafeLinksAreRemoved() {

        // Verify that real, safe URLs are rendered as links.
        Assert.assertEquals("http://full-local-allowed", getDecoratorUrl("http://full-local-allowed"));
        Assert.assertEquals("http://full-url-allowed.com", getDecoratorUrl("http://full-url-allowed.com"));
        Assert.assertEquals("http://full-url-allowed.com/test", getDecoratorUrl("http://full-url-allowed.com/test"));
        Assert.assertEquals("http://full-url-allowed.com/test?with=param", getDecoratorUrl("http://full-url-allowed.com/test?with=param"));
        Assert.assertEquals("https://https-is-allowed-too.com", getDecoratorUrl("https://https-is-allowed-too.com"));
        Assert.assertEquals("HTTPS://upper-case-https-all-good.com", getDecoratorUrl("HTTPS://upper-case-https-all-good.com"));

        // Links with double slashes should be allowed.
        Assert.assertEquals("https://graylog.com//releases", getDecoratorUrl("https://graylog.com//releases"));

        // Verify that unsafe URLs are rendered as text.
        Assert.assertEquals("javascript:alert('Javascript is not allowed.')", getDecoratorMessage("javascript:alert('Javascript is not allowed.')"));
        Assert.assertEquals("alert('Javascript this way is still not allowed", getDecoratorMessage("alert('Javascript this way is still not allowed"));
        Assert.assertEquals("ntp://other-stuff-is-not-allowed", getDecoratorMessage("ntp://other-stuff-is-not-allowed"));
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

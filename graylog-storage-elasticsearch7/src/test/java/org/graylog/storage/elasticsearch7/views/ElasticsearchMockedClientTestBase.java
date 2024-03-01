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
package org.graylog.storage.elasticsearch7.views;

import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.search.MultiSearchResponse;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.support.PlainActionFuture;
import org.graylog.storage.elasticsearch7.ElasticsearchClient;
import org.mockito.Mock;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class ElasticsearchMockedClientTestBase {

    @Mock
    protected ElasticsearchClient client;

    void mockCancellableMSearch(final MultiSearchResponse response) throws Exception {
        PlainActionFuture<MultiSearchResponse> plainActionFuture = mock(PlainActionFuture.class);
        doReturn(response).when(plainActionFuture).get();
        doReturn(plainActionFuture).when(client).cancellableMsearch(any());
    }
}

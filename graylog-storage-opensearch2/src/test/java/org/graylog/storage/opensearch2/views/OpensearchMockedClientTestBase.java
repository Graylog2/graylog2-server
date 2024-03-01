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
package org.graylog.storage.opensearch2.views;

import org.graylog.shaded.opensearch2.org.opensearch.action.search.MultiSearchResponse;
import org.graylog.shaded.opensearch2.org.opensearch.action.support.PlainActionFuture;
import org.graylog.storage.opensearch2.OpenSearchClient;
import org.mockito.Mock;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class OpensearchMockedClientTestBase {

    @Mock
    protected OpenSearchClient client;

    void mockCancellableMSearch(final MultiSearchResponse response) throws Exception {
        PlainActionFuture<MultiSearchResponse> plainActionFuture = mock(PlainActionFuture.class);
        doReturn(response).when(plainActionFuture).get();
        doReturn(plainActionFuture).when(client).cancellableMsearch(any());
    }
}

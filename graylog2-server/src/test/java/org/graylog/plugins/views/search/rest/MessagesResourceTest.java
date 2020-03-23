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
package org.graylog.plugins.views.search.rest;

import org.graylog.plugins.views.search.export.ChunkedResult;
import org.graylog.plugins.views.search.export.MessagesRequest;
import org.graylog.plugins.views.search.export.MessagesResult;
import org.graylog.plugins.views.search.export.SearchTypeExporter;
import org.graylog.plugins.views.search.export.SearchTypeOverrides;
import org.graylog2.shared.bindings.GuiceInjectorHolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.Response;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MessagesResourceTest {

    private MessagesResource sut;
    private SearchTypeExporter exporter;

    @BeforeEach
    void setUp() {
        GuiceInjectorHolder.createInjector(Collections.emptyList());
        exporter = mock(SearchTypeExporter.class);
        sut = new MessagesResource(exporter);
    }

    @Test
    void triggersBulkExport() {
        MessagesRequest request = validRequest();
        mockResultFor(request);

        Response response = sut.retrieve(request);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getEntity()).isNotNull();
    }

    @Test
    void responseHasFilenameHeader() {
        MessagesRequest request = validRequest();
        MessagesResult result = mockResultFor(request);

        Response response = sut.retrieve(request);

        assertThat((String) response.getHeaders().getFirst("Content-Disposition"))
                .as("has header Content-Disposition")
                .endsWith(result.filename());
    }

    @Test
    void triggersSearchTypeExport() {
        SearchTypeOverrides request = validOverrides();
        mockResultFor(request);

        Response response = sut.retrieveForSearchType("search-id", "search-type-id", request);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getEntity()).isNotNull();
    }

    private MessagesResult mockResultFor(MessagesRequest request) {
        MessagesResult result = someValidResult();
        when(exporter.export(request)).thenReturn(result);
        return result;
    }

    private MessagesResult someValidResult() {
        return MessagesResult.builder().filename("hasi").messages(mock(ChunkedResult.class)).build();
    }

    private MessagesRequest validRequest() {
        return MessagesRequest.builder().build();
    }

    private MessagesResult mockResultFor(SearchTypeOverrides overrides) {
        MessagesResult result = someValidResult();
        when(exporter.export(any(), any(), eq(overrides))).thenReturn(result);
        return result;
    }

    private SearchTypeOverrides validOverrides() {
        return SearchTypeOverrides.builder().build();
    }
}

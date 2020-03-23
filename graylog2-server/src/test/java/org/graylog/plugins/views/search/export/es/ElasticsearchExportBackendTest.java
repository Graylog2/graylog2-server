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
package org.graylog.plugins.views.search.export.es;

import com.google.common.collect.ImmutableSet;
import io.searchbox.client.JestClient;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import org.graylog.plugins.views.search.elasticsearch.IndexLookup;
import org.graylog.plugins.views.search.export.MessagesRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import javax.validation.ValidationException;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.graylog.plugins.views.search.export.TestData.defaultMessagesRequest;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ElasticsearchExportBackendTest {
    private JestClient client;
    private ElasticsearchExportBackend sut;
    private IndexLookup indexLookup;

    @BeforeEach
    void setUp() {
        indexLookup = mock(IndexLookup.class);
        client = mock(JestClient.class);
        sut = new ElasticsearchExportBackend(client, indexLookup);
    }

    @Test
    void usesCorrectIndices() {
        when(indexLookup.indexNamesForStreamsInTimeRange(any(), any()))
                .thenReturn(ImmutableSet.of("hasi", "mausi"));

        MessagesRequest request = defaultMessagesRequest();

        ArgumentCaptor<Search> captor = ArgumentCaptor.forClass(Search.class);
        try {
            SearchResult searchResult = mock(SearchResult.class);
            when(searchResult.isSucceeded()).thenReturn(true);
            when(client.execute(captor.capture())).thenReturn(searchResult);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        sut.run(request);

        assertThat(captor.getValue().getIndex()).isEqualTo("hasi,mausi");
    }

    @Test
    void ensuresRequestIsComplete() {
        MessagesRequest request = MessagesRequest.empty();

        assertThatExceptionOfType(ValidationException.class)
                .isThrownBy(() -> sut.run(request));
    }
}

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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.searchbox.core.Search;
import org.graylog.plugins.views.search.elasticsearch.IndexLookup;
import org.graylog.plugins.views.search.export.ExportMessagesCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.testcontainers.shaded.com.google.common.collect.ImmutableSet;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ElasticsearchExportBackendTest {

    private RequestStrategy requestStrategy;
    private ElasticsearchExportBackend sut;

    @BeforeEach
    void setUp() {
        requestStrategy = mock(RequestStrategy.class);
        when(requestStrategy.configure(any())).then(returnsFirstArg());
        sut = new ElasticsearchExportBackend(mock(IndexLookup.class), requestStrategy, false);
    }

    @Test
    void appliesRequestStrategyStreamFilter() {
        ExportMessagesCommand command = ExportMessagesCommand.withDefaults().toBuilder()
                .streams("stream-1", "stream-2").build();

        when(requestStrategy.removeUnsupportedStreams(command.streams()))
                .thenReturn(ImmutableSet.of("stream-1"));

        sut.run(command, chunk -> {
        });

        String searchPayload = captureSearchPayload(command);
        assertThat(searchPayload).doesNotContain("stream-2");
    }

    private String captureSearchPayload(ExportMessagesCommand command) {
        ArgumentCaptor<Search.Builder> captor = ArgumentCaptor.forClass(Search.Builder.class);
        verify(requestStrategy).nextChunk(captor.capture(), eq(command));

        try {
            return captor.getValue().build().getData(new ObjectMapper());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

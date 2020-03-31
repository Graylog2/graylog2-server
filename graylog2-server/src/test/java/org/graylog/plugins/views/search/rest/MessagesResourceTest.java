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

import org.glassfish.jersey.server.ChunkedOutput;
import org.graylog.plugins.views.search.export.ChunkForwarder;
import org.graylog.plugins.views.search.export.MessagesExporter;
import org.graylog.plugins.views.search.export.MessagesRequest;
import org.graylog.plugins.views.search.export.ResultFormat;
import org.graylog2.shared.bindings.GuiceInjectorHolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.io.IOException;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class MessagesResourceTest {

    private MessagesResource sut;
    private MessagesExporter exporter;

    @BeforeEach
    void setUp() {
        GuiceInjectorHolder.createInjector(Collections.emptyList());
        exporter = mock(MessagesExporter.class);
        sut = new MessagesResource(exporter);
        sut.asyncRunner = Runnable::run;
    }

    @Test
    void writesToChunkedOutputAsResultsComeIn() throws IOException {
        MessagesRequest request = validRequest();

        @SuppressWarnings("unchecked") ChunkedOutput<String> output = mock(ChunkedOutput.class);

        sut.chunkedOutputSupplier = () -> output;

        ArgumentCaptor<ChunkForwarder<String>> chunkForwarderArgumentCaptor = ArgumentCaptor.forClass(ChunkForwarder.class);
        doNothing().when(exporter).export(any(), chunkForwarderArgumentCaptor.capture());

        sut.retrieve(request);

        verify(output, never()).write(any());

        ChunkForwarder<String> forwarder = chunkForwarderArgumentCaptor.getValue();

        forwarder.write("chunk-1");
        forwarder.write("chunk-2");
        forwarder.close();

        InOrder verifier = inOrder(output);

        verifier.verify(output).write("chunk-1");
        verifier.verify(output).write("chunk-2");
        verifier.verify(output).close();
    }

    //TODO: reimplement this stuff
//    @Test
//    void triggersBulkExport() {
//        MessagesRequest request = validRequest();
//
//        Response response = sut.retrieve(request);
//
//        assertThat(response.getStatus()).isEqualTo(200);
//        assertThat(response.getEntity()).isNotNull();
//    }
//
//    @Test
//    void responseHasFilenameHeader() {
//        MessagesRequest request = validRequest();
//
//        Response response = sut.retrieve(request);
//
//        assertThat((String) response.getHeaders().getFirst("Content-Disposition"))
//                .as("has header Content-Disposition")
//                .endsWith(result.filename());
//    }
//
//    @Test
//    void triggersSearchTypeExport() {
//        SearchTypeOverrides request = validOverrides();
//
//        Response response = sut.retrieveForSearchType("search-id", "search-type-id", request);
//
//        assertThat(response.getStatus()).isEqualTo(200);
//        assertThat(response.getEntity()).isNotNull();
//    }
//
    private MessagesRequest validRequest() {
        return MessagesRequest.builder().build();
    }

    private ResultFormat validOverrides() {
        return ResultFormat.builder().build();
    }
}

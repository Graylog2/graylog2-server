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

import com.google.common.collect.ImmutableSet;
import org.glassfish.jersey.server.ChunkedOutput;
import org.graylog.plugins.views.search.SearchDomain;
import org.graylog.plugins.views.search.SearchExecutionGuard;
import org.graylog.plugins.views.search.errors.PermissionException;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MessagesResourceTest {

    private MessagesResource sut;
    private MessagesExporter exporter;
    private PermittedStreams permittedStreams;
    private SearchExecutionGuard executionGuard;

    @BeforeEach
    void setUp() {
        GuiceInjectorHolder.createInjector(Collections.emptyList());
        exporter = mock(MessagesExporter.class);
        permittedStreams = mock(PermittedStreams.class);
        when(permittedStreams.load(any())).thenReturn(ImmutableSet.of("a-default-stream"));
        executionGuard = mock(SearchExecutionGuard.class);
        sut = new MessagesResource(exporter, mock(SearchDomain.class), executionGuard, permittedStreams);
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

    @Test
    void appliesDefaultStreamsToRequestIfOmitted() {
        MessagesRequest request = validRequest();

        when(permittedStreams.load(any())).thenReturn(ImmutableSet.of("stream-1", "stream-2"));

        ArgumentCaptor<MessagesRequest> captor = ArgumentCaptor.forClass(MessagesRequest.class);

        doNothing().when(exporter).export(captor.capture(), any());

        sut.retrieve(request);

        assertThat(captor.getValue().streams())
                .contains(ImmutableSet.of("stream-1", "stream-2"));
    }

    @Test
    void checksStreamPermissionsForPlainRequest() {
        MessagesRequest request = validRequest().toBuilder().streams(ImmutableSet.of("stream-1")).build();

        PermissionException exception = new PermissionException("The wurst is yet to come");
        doThrow(exception).when(executionGuard)
                .checkUserIsPermittedToSeeStreams(eq(ImmutableSet.of("stream-1")), any());

        assertThatExceptionOfType(PermissionException.class).isThrownBy(() -> sut.retrieve(request))
                .withMessageContaining(exception.getMessage());
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

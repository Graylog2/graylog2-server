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
import org.graylog.plugins.views.search.SearchDomain;
import org.graylog.plugins.views.search.SearchExecutionGuard;
import org.graylog.plugins.views.search.errors.PermissionException;
import org.graylog.plugins.views.search.export.MessagesExporter;
import org.graylog.plugins.views.search.export.MessagesRequest;
import org.graylog2.shared.bindings.GuiceInjectorHolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
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

        sut.asyncRunner = c -> {
            c.accept(x -> {
            });
            return null;
        };
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

    private MessagesRequest validRequest() {
        return MessagesRequest.builder().build();
    }
}

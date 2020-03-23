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
package org.graylog.plugins.views.search.export;

import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MessagesExporterTest {

    private ExportBackend backend;
    private Defaults defaults;
    private SearchTypeExporter sut;

    @BeforeEach
    void setUp() {
        defaults = mock(Defaults.class);
        backend = mock(ExportBackend.class);
        sut = new SearchTypeExporter(defaults, backend);
    }

    @Test
    void returnsBackendResult() {
        MessagesRequest request = MessagesRequest.builder().build();
        MessagesRequest requestWithDefaultsFilledIn = MessagesRequest.builder().fieldsInOrder(ImmutableSet.of("hansimann")).build();

        when(defaults.fillInIfNecessary(request)).thenReturn(requestWithDefaultsFilledIn);

        ChunkedResult expected = mock(ChunkedResult.class);
        when(backend.run(requestWithDefaultsFilledIn)).thenReturn(expected);

        MessagesResult result = sut.export(request);

        assertThat(result.messages()).isEqualTo(expected);
    }
}

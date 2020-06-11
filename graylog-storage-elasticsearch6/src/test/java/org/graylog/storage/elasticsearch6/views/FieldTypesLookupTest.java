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
package org.graylog.storage.elasticsearch6.views;

import com.google.common.collect.ImmutableSet;
import org.graylog.plugins.views.search.elasticsearch.FieldTypesLookup;
import org.graylog2.indexer.fieldtypes.FieldTypeDTO;
import org.graylog2.indexer.fieldtypes.IndexFieldTypesDTO;
import org.graylog2.indexer.fieldtypes.IndexFieldTypesService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FieldTypesLookupTest {
    private IndexFieldTypesService indexFieldTypesService;
    private FieldTypesLookup fieldTypesLookup;

    @BeforeEach
    void setUp() {
        this.indexFieldTypesService = mock(IndexFieldTypesService.class);
        this.fieldTypesLookup = new FieldTypesLookup(indexFieldTypesService);
    }

    @Test
    void returnsEmptyOptionalIfFieldTypesAreEmpty() {
        final Optional<String> result = this.fieldTypesLookup.getType(Collections.singleton("SomeStream"), "somefield");
        assertThat(result).isEmpty();
    }

    @Test
    void returnsEmptyOptionalIfStreamsAreEmpty() {
        final Optional<String> result = this.fieldTypesLookup.getType(Collections.emptySet(), "somefield");
        assertThat(result).isEmpty();
    }

    @Test
    void returnsEmptyOptionalIfMultipleTypesExistForField() {
        when(this.indexFieldTypesService.findForStreamIds(Collections.singleton("stream1"))).thenReturn(ImmutableSet.of(
                IndexFieldTypesDTO.create("indexSet1", "stream1", ImmutableSet.of(
                        FieldTypeDTO.create("somefield", "long")
                )),
                IndexFieldTypesDTO.create("indexSet2", "stream1", ImmutableSet.of(
                        FieldTypeDTO.create("somefield", "float")
                ))
        ));

        final Optional<String> result = this.fieldTypesLookup.getType(Collections.singleton("stream1"), "somefield");
        assertThat(result).isEmpty();
    }

    @Test
    void returnsEmptyOptionalIfNoTypesExistForStream() {
        when(this.indexFieldTypesService.findForStreamIds(Collections.singleton("stream1"))).thenReturn(ImmutableSet.of(
                IndexFieldTypesDTO.create("indexSet1", "stream1", ImmutableSet.of(
                        FieldTypeDTO.create("somefield", "long")
                ))
        ));

        final Optional<String> result = this.fieldTypesLookup.getType(Collections.singleton("stream2"), "somefield");
        assertThat(result).isEmpty();
    }

    @Test
    void returnsFieldTypeIfSingleTypeExistsForFieldInStream() {
        when(this.indexFieldTypesService.findForStreamIds(Collections.singleton("stream1"))).thenReturn(ImmutableSet.of(
                IndexFieldTypesDTO.create("indexSet1", "stream1", ImmutableSet.of(
                        FieldTypeDTO.create("somefield", "long")
                ))
        ));

        final Optional<String> result = this.fieldTypesLookup.getType(Collections.singleton("stream1"), "somefield");
        assertThat(result).contains("long");
    }

    @Test
    void returnsFieldTypeIfSingleTypeExistsForFieldInAllStreams() {
        when(this.indexFieldTypesService.findForStreamIds(ImmutableSet.of("stream1", "stream2"))).thenReturn(ImmutableSet.of(
                IndexFieldTypesDTO.create("indexSet1", "stream1", ImmutableSet.of(
                        FieldTypeDTO.create("somefield", "long")
                )),
                IndexFieldTypesDTO.create("indexSet2", "stream2", ImmutableSet.of(
                        FieldTypeDTO.create("somefield", "long")
                ))
        ));

        final Optional<String> result = this.fieldTypesLookup.getType(ImmutableSet.of("stream1", "stream2"), "somefield");
        assertThat(result).contains("long");
    }
}

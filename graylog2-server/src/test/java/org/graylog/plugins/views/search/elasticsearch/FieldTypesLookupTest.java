package org.graylog.plugins.views.search.elasticsearch;

import com.google.common.collect.ImmutableSet;
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

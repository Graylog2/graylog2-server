package org.graylog2.indexer.fieldtypes;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.graylog.plugins.views.search.rest.MappedFieldTypeDTO;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.streams.StreamService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MappedFieldTypesServiceTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private StreamService streamService;

    @Mock
    private IndexFieldTypesService indexFieldTypesService;

    private MappedFieldTypesService mappedFieldTypesService;

    @Before
    public void setUp() throws Exception {
        this.mappedFieldTypesService = new MappedFieldTypesService(streamService, indexFieldTypesService, new FieldTypeMapper());
        final Stream stream = mock(Stream.class, Answers.RETURNS_DEEP_STUBS);
        when(stream.getIndexSet().getConfig().id()).thenReturn("indexSetId");
        final Set<Stream> streams = Collections.singleton(stream);
        when(streamService.loadByIds(Collections.singleton("stream1"))).thenReturn(streams);
    }

    @Test
    public void fieldsOfSameTypeDoNotReturnCompoundTypeIfPropertiesAreDifferent() {
        final List<IndexFieldTypesDTO> fieldTypes = ImmutableList.of(
                createIndexTypes(
                        "deadbeef",
                        "testIndex",
                        FieldTypeDTO.create("field1", "keyword"),
                        FieldTypeDTO.create("field2", "long")
                ),
                createIndexTypes(
                        "affeaffe",
                        "testIndex2",
                        FieldTypeDTO.create("field1", "text"),
                        FieldTypeDTO.create("field2", "long")
                )
        );
        when(indexFieldTypesService.findForIndexSet("indexSetId")).thenReturn(fieldTypes);

        final Set<MappedFieldTypeDTO> result = this.mappedFieldTypesService.fieldTypesByStreamIds(Collections.singleton("stream1"));
        assertThat(result).containsExactlyInAnyOrder(
                MappedFieldTypeDTO.create("field2", FieldTypes.Type.createType("long", ImmutableSet.of("numeric", "enumerable"))),
                MappedFieldTypeDTO.create("field1", FieldTypes.Type.createType("string", ImmutableSet.of("compound")))
        );
    }

    @Test
    public void fieldsOfDifferentTypesDoReturnCompoundType() {
        final List<IndexFieldTypesDTO> fieldTypes = ImmutableList.of(
                createIndexTypes(
                        "deadbeef",
                        "testIndex",
                        FieldTypeDTO.create("field1", "long"),
                        FieldTypeDTO.create("field2", "long")
                ),
                createIndexTypes(
                        "affeaffe",
                        "testIndex2",
                        FieldTypeDTO.create("field1", "text"),
                        FieldTypeDTO.create("field2", "long")
                )
        );
        when(indexFieldTypesService.findForIndexSet("indexSetId")).thenReturn(fieldTypes);

        final Set<MappedFieldTypeDTO> result = this.mappedFieldTypesService.fieldTypesByStreamIds(Collections.singleton("stream1"));
        assertThat(result).containsExactlyInAnyOrder(
                MappedFieldTypeDTO.create("field2", FieldTypes.Type.createType("long", ImmutableSet.of("numeric", "enumerable"))),
                MappedFieldTypeDTO.create("field1", FieldTypes.Type.createType("compound(long,string)", ImmutableSet.of("compound")))
        );
    }

    private IndexFieldTypesDTO createIndexTypes(String indexId, String indexName, FieldTypeDTO... fieldTypes) {
        return IndexFieldTypesDTO.create(indexId, indexName, java.util.stream.Stream.of(fieldTypes).collect(Collectors.toSet()));
    }
}

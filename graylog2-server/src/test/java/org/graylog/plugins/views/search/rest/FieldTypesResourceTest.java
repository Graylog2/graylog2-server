package org.graylog.plugins.views.search.rest;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.apache.shiro.subject.Subject;
import org.graylog2.indexer.fieldtypes.FieldTypeDTO;
import org.graylog2.indexer.fieldtypes.FieldTypeMapper;
import org.graylog2.indexer.fieldtypes.FieldTypes;
import org.graylog2.indexer.fieldtypes.IndexFieldTypesDTO;
import org.graylog2.indexer.fieldtypes.IndexFieldTypesService;
import org.graylog2.shared.security.RestPermissions;
import org.graylog2.streams.StreamService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class FieldTypesResourceTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private IndexFieldTypesService indexFieldTypesService;

    @Mock
    private StreamService streamService;

    @Mock
    private Subject currentSubject;

    class FieldTypesTestResource extends FieldTypesResource {
        FieldTypesTestResource(IndexFieldTypesService indexFieldTypesService, StreamService streamService, FieldTypeMapper fieldTypeMapper) {
            super(indexFieldTypesService, streamService, fieldTypeMapper);
        }

        @Override
        protected Subject getSubject() {
            return currentSubject;
        }
    }

    private FieldTypesResource fieldTypesResource;

    @Before
    public void setUp() throws Exception {
        when(currentSubject.isPermitted(eq(RestPermissions.STREAMS_READ + ":*"))).thenReturn(true);
        this.fieldTypesResource = new FieldTypesTestResource(indexFieldTypesService, streamService, new FieldTypeMapper());
    }

    private IndexFieldTypesDTO createIndexTypes(String indexId, String indexName, FieldTypeDTO... fieldTypes) {
        return IndexFieldTypesDTO.create(indexId, indexName, Stream.of(fieldTypes).collect(Collectors.toSet()));
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
        when(indexFieldTypesService.findAll()).thenReturn(fieldTypes);

        final Set<MappedFieldTypeDTO> result = this.fieldTypesResource.allFieldTypes();
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
        when(indexFieldTypesService.findAll()).thenReturn(fieldTypes);

        final Set<MappedFieldTypeDTO> result = this.fieldTypesResource.allFieldTypes();
        assertThat(result).containsExactlyInAnyOrder(
                MappedFieldTypeDTO.create("field2", FieldTypes.Type.createType("long", ImmutableSet.of("numeric", "enumerable"))),
                MappedFieldTypeDTO.create("field1", FieldTypes.Type.createType("compound(long,string)", ImmutableSet.of("compound")))
        );
    }
}

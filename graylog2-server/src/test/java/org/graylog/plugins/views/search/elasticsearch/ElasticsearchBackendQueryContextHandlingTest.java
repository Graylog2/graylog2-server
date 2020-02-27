package org.graylog.plugins.views.search.elasticsearch;

import com.google.common.collect.ImmutableSet;
import io.searchbox.client.JestClient;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.SearchJob;
import org.graylog.plugins.views.search.SearchType;
import org.graylog.plugins.views.search.elasticsearch.searchtypes.ESSearchTypeHandler;
import org.graylog2.indexer.fieldtypes.FieldTypeDTO;
import org.graylog2.indexer.fieldtypes.IndexFieldTypesDTO;
import org.graylog2.indexer.fieldtypes.IndexFieldTypesService;
import org.graylog2.indexer.ranges.IndexRangeService;
import org.graylog2.plugin.indexer.searches.timeranges.InvalidRangeParametersException;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.graylog2.streams.StreamService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ElasticsearchBackendQueryContextHandlingTest extends ElasticsearchBackendTestBase {
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private ESSearchTypeHandler<SearchType> searchTypeHandler;

    @Mock
    private Query query;

    @Mock
    private SearchType searchType;

    @Mock
    private IndexFieldTypesService indexFieldTypesService;

    private ElasticsearchBackend backend;

    @Before
    public void setUp() throws Exception {
        final ESQueryDecorators esQueryDecorators = mock(ESQueryDecorators.class);
        when(esQueryDecorators.decorate(any(), any(), any(), any())).thenReturn("");
        this.backend = new ElasticsearchBackend(
                Collections.singletonMap("dummy", () -> searchTypeHandler),
                new QueryStringParser(),
                mock(JestClient.class),
                mock(IndexRangeService.class),
                mock(StreamService.class),
                esQueryDecorators,
                indexFieldTypesService
        );

        final ElasticsearchQueryString backendQuery = ElasticsearchQueryString.builder().queryString("").build();
        when(searchType.type()).thenReturn("dummy");
        when(searchType.effectiveStreams()).thenReturn(Collections.singleton("stream1"));
        final ImmutableSet<SearchType> searchTypes = ImmutableSet.of(searchType);
        when(query.query()).thenReturn(backendQuery);
        when(query.searchTypes()).thenReturn(searchTypes);
        when(query.effectiveTimeRange(searchType)).thenReturn(RelativeRange.create(300));
    }

    @Test
    public void generateAddsFieldTypesOfCurrentScopeToContextIfPresent() throws InvalidRangeParametersException {
        returnFieldTypes(
                Collections.singleton(FieldTypeDTO.builder()
                        .fieldName("somefield")
                        .physicalType("string")
                        .build())
        );

        this.backend.generate(mock(SearchJob.class), query, Collections.emptySet());

        assertThat(providedFieldTypes()).containsExactly(
                new AbstractMap.SimpleEntry<>("somefield", Collections.singleton("string"))
        );
    }

    @Test
    public void generateAddsFieldTypesEvenIfEmpty() throws InvalidRangeParametersException {
        returnFieldTypes(Collections.emptySet());

        this.backend.generate(mock(SearchJob.class), query, Collections.emptySet());

        assertThat(providedFieldTypes()).isEmpty();
    }

    @Test
    public void generateAddsFieldTypesForAmbiguousFieldTypes() throws InvalidRangeParametersException {
        returnFieldTypes(
                ImmutableSet.of(
                        FieldTypeDTO.builder()
                                .fieldName("somefield")
                                .physicalType("string")
                                .build(),
                        FieldTypeDTO.builder()
                                .fieldName("somefield")
                                .physicalType("long")
                                .build()
                )
        );

        this.backend.generate(mock(SearchJob.class), query, Collections.emptySet());

        assertThat(providedFieldTypes()).containsExactly(
                new AbstractMap.SimpleEntry<>("somefield", ImmutableSet.of("string", "long"))
        );
    }

    private Map<String, Set<String>> providedFieldTypes() {
        final ArgumentCaptor<ESGeneratedQueryContext> contextCaptor = ArgumentCaptor.forClass(ESGeneratedQueryContext.class);
        verify(searchTypeHandler, times(1)).generateQueryPart(any(), any(), eq(searchType), contextCaptor.capture());
        final ESGeneratedQueryContext queryContext = contextCaptor.getValue();
        return queryContext.fieldTypes();
    }

    private void returnFieldTypes(Set<FieldTypeDTO> fieldTypes) {
        final IndexFieldTypesDTO indexFieldTypes = IndexFieldTypesDTO.create(
                "indexSetId1",
                "indexSet",
                fieldTypes
        );
        when(indexFieldTypesService.findForStreamIds(Collections.singleton("stream1"))).thenReturn(Collections.singleton(indexFieldTypes));
    }
}

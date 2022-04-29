package org.graylog.storage.elasticsearch7.fieldtypes.streams;

import one.util.streamex.EntryStream;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.search.MultiSearchResponse;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.search.SearchRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.builder.SearchSourceBuilder;
import org.graylog.storage.elasticsearch7.ElasticsearchClient;
import org.graylog.storage.elasticsearch7.SearchRequestFactory;
import org.graylog2.indexer.fieldtypes.FieldTypeDTO;
import org.graylog2.indexer.fieldtypes.streams.CountExistingBasedFieldTypeFilterAdapter;
import org.graylog2.indexer.searches.SearchesConfig;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class CountExistingBasedFieldTypeFilterES7 implements CountExistingBasedFieldTypeFilterAdapter {

    private final ElasticsearchClient client;
    private final SearchRequestFactory searchRequestFactory;

    @Inject
    public CountExistingBasedFieldTypeFilterES7(final ElasticsearchClient client, final SearchRequestFactory searchRequestFactory) {
        this.client = client;
        this.searchRequestFactory = searchRequestFactory;
    }

    @Override
    public Set<FieldTypeDTO> filterFieldTypes(final Set<FieldTypeDTO> fieldTypeDTOs, final Set<String> indexNames, final Collection<String> streamIds) {
        if (indexNames == null || indexNames.isEmpty()) {
            return Collections.emptySet();
        }
        if (streamIds == null || streamIds.isEmpty()) {
            return fieldTypeDTOs;
        }
        if (fieldTypeDTOs == null || fieldTypeDTOs.isEmpty()) {
            return fieldTypeDTOs;
        }

        List<FieldTypeDTO> orderedFieldTypeDTOs = new ArrayList<>(fieldTypeDTOs);
        final List<MultiSearchResponse.Item> msearchResponse = client.msearch(orderedFieldTypeDTOs.stream()
                        .map(f -> buildSearchRequestForParticularFieldExistence(f, indexNames, streamIds))
                        .collect(Collectors.toList()),
                "Unable to retrieve existing text fields types");

        final Set<FieldTypeDTO> filteredFields = EntryStream.of(orderedFieldTypeDTOs)
                .filterKeyValue((i, field) -> msearchResponse.get(i).getResponse().getHits().getTotalHits().value > 0)
                .values()
                .collect(Collectors.toSet());

        return filteredFields;
    }

    private SearchRequest buildSearchRequestForParticularFieldExistence(final FieldTypeDTO fieldType, final Set<String> indexNames, final Collection<String> streamIds) {
        final SearchesConfig config = SearchesConfig.builder()
                .query("")
                .filter(buildFilterFromStreamsAndField(streamIds, fieldType))
                .range(RelativeRange.allTime())
                .limit(0)
                .offset(0)
                .build();
        final SearchSourceBuilder searchSourceBuilder = searchRequestFactory
                .create(config)
                .trackTotalHitsUpTo(1)
                .terminateAfter(1);

        return new SearchRequest(indexNames.toArray(new String[0]))
                .source(searchSourceBuilder);
    }

    private String buildFilterFromStreamsAndField(final Collection<String> streamIds, final FieldTypeDTO fieldType) {
        String filter = "(" + streamIds.stream()
                .map(id -> "streams:" + id)
                .collect(Collectors.joining(" OR ")) + ")";

        return filter + "AND _exists_:" + fieldType.fieldName();
    }
}

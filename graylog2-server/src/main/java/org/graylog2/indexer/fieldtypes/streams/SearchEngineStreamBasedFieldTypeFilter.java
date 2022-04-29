package org.graylog2.indexer.fieldtypes.streams;

import org.graylog2.indexer.fieldtypes.FieldTypeDTO;

import javax.inject.Inject;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Tries to query the search engine (ES/Opensearch) to find out which fields are relevant for the streams.
 */
public class SearchEngineStreamBasedFieldTypeFilter implements StreamBasedFieldTypeFilter {

    private final AggregationBasedFieldTypeFilterAdapter aggregationBasedFieldTypeFilterAdapter;
    private final CountExistingBasedFieldTypeFilterAdapter countExistingBasedFieldTypeFilterAdapter;

    @Inject
    public SearchEngineStreamBasedFieldTypeFilter(final AggregationBasedFieldTypeFilterAdapter aggregationBasedFieldTypeFilterAdapter,
                                                  final CountExistingBasedFieldTypeFilterAdapter countExistingBasedFieldTypeFilterAdapter) {
        this.aggregationBasedFieldTypeFilterAdapter = aggregationBasedFieldTypeFilterAdapter;
        this.countExistingBasedFieldTypeFilterAdapter = countExistingBasedFieldTypeFilterAdapter;
    }

    @Override
    public Set<FieldTypeDTO> filterFieldTypes(final Set<FieldTypeDTO> fieldTypeDTOs, final Set<String> indexNames, final Collection<String> streamIds) {
        final Set<FieldTypeDTO> textFields = fieldTypeDTOs.stream()
                .filter(fieldTypeDTO -> fieldTypeDTO.physicalType().equals("text"))
                .collect(Collectors.toSet());

        final Set<FieldTypeDTO> nonTextFields = new HashSet<>(fieldTypeDTOs);
        nonTextFields.removeAll(textFields);

        Set<FieldTypeDTO> filtered = new HashSet<>();
        filtered.addAll(aggregationBasedFieldTypeFilterAdapter.filterFieldTypes(nonTextFields, indexNames, streamIds));
        filtered.addAll(countExistingBasedFieldTypeFilterAdapter.filterFieldTypes(textFields, indexNames, streamIds));
        return filtered;
    }
}

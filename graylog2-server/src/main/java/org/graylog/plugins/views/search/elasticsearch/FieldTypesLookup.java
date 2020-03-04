package org.graylog.plugins.views.search.elasticsearch;

import com.google.common.collect.Sets;
import org.graylog2.indexer.fieldtypes.FieldTypeDTO;
import org.graylog2.indexer.fieldtypes.IndexFieldTypesService;

import javax.inject.Inject;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class FieldTypesLookup {
    private final IndexFieldTypesService indexFieldTypesService;

    @Inject
    public FieldTypesLookup(IndexFieldTypesService indexFieldTypesService) {
        this.indexFieldTypesService = indexFieldTypesService;
    }

    public Map<String, Set<String>> get(Set<String> streamIds) {
        return this.indexFieldTypesService.findForStreamIds(streamIds)
                .stream()
                .flatMap(indexFieldTypes -> indexFieldTypes.fields().stream())
                .collect(Collectors.toMap(
                        FieldTypeDTO::fieldName,
                        fieldType -> Collections.singleton(fieldType.physicalType()),
                        Sets::union
                ));
    }
}

package org.graylog.storage.elasticsearch6.fieldtypes.streams;

import org.graylog2.indexer.fieldtypes.FieldTypeDTO;
import org.graylog2.indexer.fieldtypes.streams.AggregationBasedFieldTypeFilterAdapter;

import java.util.Collection;
import java.util.Set;

public class AggregationBasedFieldTypeFilterES6 implements AggregationBasedFieldTypeFilterAdapter {

    @Override
    public Set<FieldTypeDTO> filterFieldTypes(final Set<FieldTypeDTO> fieldTypeDTOs,
                                              final Set<String> indexNames,
                                              final Collection<String> streamIds) {
        return fieldTypeDTOs; //TODO: implement
    }
}

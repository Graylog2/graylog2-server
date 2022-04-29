package org.graylog2.indexer.fieldtypes.streams;

import org.graylog2.indexer.fieldtypes.FieldTypeDTO;

import java.util.Collection;
import java.util.Set;

public interface AggregationBasedFieldTypeFilterAdapter {

    Set<FieldTypeDTO> filterFieldTypes(final Set<FieldTypeDTO> fieldTypeDTOs, final Set<String> indexNames, final Collection<String> streamIds);
}

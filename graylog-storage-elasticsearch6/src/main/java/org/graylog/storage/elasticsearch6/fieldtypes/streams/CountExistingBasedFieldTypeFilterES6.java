package org.graylog.storage.elasticsearch6.fieldtypes.streams;

import org.graylog2.indexer.fieldtypes.FieldTypeDTO;
import org.graylog2.indexer.fieldtypes.streams.CountExistingBasedFieldTypeFilterAdapter;

import java.util.Collection;
import java.util.Set;

public class CountExistingBasedFieldTypeFilterES6 implements CountExistingBasedFieldTypeFilterAdapter {

    @Override
    public Set<FieldTypeDTO> filterFieldTypes(Set<FieldTypeDTO> fieldTypeDTOs, Set<String> indexNames, Collection<String> streamIds) {
        return fieldTypeDTOs; //TODO: implement
    }
}

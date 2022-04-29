package org.graylog2.indexer.fieldtypes.streams;

import org.graylog2.indexer.fieldtypes.FieldTypeDTO;

import java.util.Collection;
import java.util.Set;

/**
 * Does not filter fields, returns them all.
 */
public class AllowAllStreamBasedFieldTypeFilter implements StreamBasedFieldTypeFilter {

    @Override
    public Set<FieldTypeDTO> filterFieldTypes(final Set<FieldTypeDTO> fieldTypeDTOs, final Set<String> indexNames, final Collection<String> streamIds) {
        return fieldTypeDTOs;
    }
}

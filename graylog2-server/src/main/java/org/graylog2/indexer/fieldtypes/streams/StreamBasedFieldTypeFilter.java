package org.graylog2.indexer.fieldtypes.streams;

import org.graylog2.indexer.fieldtypes.FieldTypeDTO;

import java.util.Collection;
import java.util.Set;

/**
 * Filters field types so that only the ones relevant to particular streams are returned.
 */
public interface StreamBasedFieldTypeFilter {

    Set<FieldTypeDTO> filterFieldTypes(final Set<FieldTypeDTO> fieldTypeDTOs, final Set<String> indexNames, final Collection<String> streamIds);
}

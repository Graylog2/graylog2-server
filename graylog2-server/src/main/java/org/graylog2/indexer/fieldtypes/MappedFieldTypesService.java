package org.graylog2.indexer.fieldtypes;

import org.graylog.plugins.views.search.rest.MappedFieldTypeDTO;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;

import java.util.Collection;
import java.util.Set;

public interface MappedFieldTypesService {
    Set<MappedFieldTypeDTO> fieldTypesByStreamIds(Collection<String> streamIds, TimeRange timeRange);
}

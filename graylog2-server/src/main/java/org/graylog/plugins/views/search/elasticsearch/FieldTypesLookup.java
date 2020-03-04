/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
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

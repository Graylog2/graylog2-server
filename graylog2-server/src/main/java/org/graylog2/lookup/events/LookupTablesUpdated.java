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
package org.graylog2.lookup.events;

import com.google.auto.value.AutoValue;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.graylog2.lookup.dto.LookupTableDto;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

@AutoValue
public abstract class LookupTablesUpdated {
    @JsonProperty("lookup_table_ids")
    public abstract Set<String> lookupTableIds();

    @JsonProperty("lookup_table_names")
    public abstract Set<String> lookupTableNames();

    @JsonCreator
    public static LookupTablesUpdated create(@JsonProperty("lookup_table_ids") Set<String> lookupTableIds,
                                             @JsonProperty("lookup_table_names") Set<String> lookupTableNames) {
        return new AutoValue_LookupTablesUpdated(lookupTableIds, lookupTableNames);
    }

    public static LookupTablesUpdated create(LookupTableDto dto) {
        return create(Collections.singleton(dto.id()), Collections.singleton(dto.name()));
    }

    public static LookupTablesUpdated create(Collection<LookupTableDto> dtos) {
        return create(dtos.stream().map(LookupTableDto::id).collect(Collectors.toSet()),
                dtos.stream().map(LookupTableDto::name).collect(Collectors.toSet()));
    }
}

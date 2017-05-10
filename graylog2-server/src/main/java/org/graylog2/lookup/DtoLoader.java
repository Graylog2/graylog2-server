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
package org.graylog2.lookup;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.inject.assistedinject.Assisted;
import org.graylog2.lookup.dto.CacheDto;
import org.graylog2.lookup.dto.DataAdapterDto;
import org.graylog2.lookup.dto.LookupTableDto;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * Loads and caches lookup table related DTOs to avoid hammering the database when loading all lookup tables for the
 * first time during server startup.
 */
public class DtoLoader {
    interface Factory {
        DtoLoader create(Collection<LookupTableDto> lookupTableDtos);
    }

    private final Map<String, CacheDto> caches;
    private final Map<String, DataAdapterDto> dataAdapters;

    @Inject
    DtoLoader(@Assisted Collection<LookupTableDto> lookupTableDtos,
              MongoLutCacheService cacheService,
              MongoLutDataAdapterService dataAdapterService) {
        final ImmutableSet.Builder<String> cacheIds = ImmutableSet.builder();
        final ImmutableSet.Builder<String> adapterIds = ImmutableSet.builder();

        lookupTableDtos.forEach(dto -> {
            cacheIds.add(dto.cacheId());
            adapterIds.add(dto.dataAdapterId());
        });

        this.caches = Maps.uniqueIndex(cacheService.findByIds(cacheIds.build()), CacheDto::id);
        this.dataAdapters = Maps.uniqueIndex(dataAdapterService.findByIds(adapterIds.build()), DataAdapterDto::id);
    }

    Optional<CacheDto> getCacheDto(String id) {
        return Optional.ofNullable(caches.get(id));
    }

    Optional<DataAdapterDto> getDataAdapterDto(String id) {
        return Optional.ofNullable(dataAdapters.get(id));
    }
}

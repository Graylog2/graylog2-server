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

import com.google.inject.assistedinject.Assisted;

import org.graylog2.lookup.dto.CacheDto;
import org.graylog2.lookup.dto.DataAdapterDto;
import org.graylog2.lookup.dto.LookupTableDto;
import org.graylog2.plugin.lookup.LookupCache;
import org.graylog2.plugin.lookup.LookupDataAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

/**
 * Responsible for creating correct {@link LookupTable} objects including data adapters and caches.
 */
class LookupTableCreator {
    private static final Logger LOG = LoggerFactory.getLogger(LookupTableCreator.class);
    private final DtoLoader dtoLoader;

    interface Factory {
        LookupTableCreator create(Collection<LookupTableDto> dtos);
    }

    private final Map<String, LookupCache.Factory> cacheFactories;
    private final Map<String, LookupDataAdapter.Factory> adapterFactories;

    @Inject
    LookupTableCreator(@Assisted Collection<LookupTableDto> dtos,
                       DtoLoader.Factory dtoLoaderFactory,
                       Map<String, LookupCache.Factory> cacheFactories,
                       Map<String, LookupDataAdapter.Factory> adapterFactories) {
        this.dtoLoader = dtoLoaderFactory.create(dtos);
        this.cacheFactories = cacheFactories;
        this.adapterFactories = adapterFactories;
    }

    Optional<LookupTable> createLookupTable(LookupTableDto dto) {
        final Optional<LookupCache> cache = createCache(dto);
        final Optional<LookupDataAdapter> dataAdapter = createDataAdapter(dto);

        if (!cache.isPresent() || !dataAdapter.isPresent()) {
            return Optional.empty();
        }

        return createLookupTable(dto, cache.get(), dataAdapter.get());
    }

    Optional<LookupTable> createLookupTable(LookupTableDto dto, LookupCache cache, LookupDataAdapter dataAdapter) {
        LookupTable lookupTable = LookupTable.builder()
                .id(dto.id())
                .name(dto.name())
                .title(dto.title())
                .description(dto.description())
                .cache(cache)
                .dataAdapter(dataAdapter)
                .build();
        // set up back references so the cache can interact with the adapter (e.g. lazily loading values)
        lookupTable.cache().setLookupTable(lookupTable);
        lookupTable.dataAdapter().setLookupTable(lookupTable);

        return Optional.of(lookupTable);
    }

    Optional<LookupCache> createCache(LookupTableDto dto) {
        Optional<CacheDto> cacheDtoOptional = dtoLoader.getCacheDto(dto.cacheId());
        if (!cacheDtoOptional.isPresent()) {
            LOG.warn("Missing lookup cache configuration for ID {} in lookup table {}.", dto.cacheId(), dto.name());
            return Optional.empty();
        }
        final CacheDto cacheDto = cacheDtoOptional.get();
        return getCacheFactory(dto.name(), cacheDto)
                .map(factory -> factory.create(cacheDto.config()));
    }

    Optional<LookupDataAdapter> createDataAdapter(LookupTableDto dto) {
        Optional<DataAdapterDto> adapterDtoOptional = dtoLoader.getDataAdapterDto(dto.dataAdapterId());
        if (!adapterDtoOptional.isPresent()) {
            LOG.warn("Missing lookup data adapter configuration for ID {} in lookup table {}.", dto.dataAdapterId(), dto.name());
            return Optional.empty();
        }
        final DataAdapterDto adapterDto = adapterDtoOptional.get();
        return getDataAdapterFactory(dto.name(), adapterDto)
                .map(factory -> {
                    final LookupDataAdapter adapter = factory.create(adapterDto.config());
                    adapter.setId(adapterDto.id());
                    return adapter;
                });
    }

    private Optional<LookupCache.Factory> getCacheFactory(String lutName, CacheDto cacheDto) {
        LookupCache.Factory cacheFactory = cacheFactories.get(cacheDto.config().type());
        if (cacheFactory == null) {
            LOG.warn("Missing lookup cache implementation for type {} in lookup table {}.", cacheDto.config().type(), lutName);
            return Optional.empty();
        }

        return Optional.of(cacheFactory);
    }

    private Optional<LookupDataAdapter.Factory> getDataAdapterFactory(String lutName, DataAdapterDto adapterDto) {
        LookupDataAdapter.Factory adapterFactory = adapterFactories.get(adapterDto.config().type());
        if (adapterFactory == null) {
            LOG.warn("Missing lookup data adapter implementation for type {} in lookup table {}.", adapterDto.config().type(), lutName);
            return Optional.empty();
        }

        return Optional.of(adapterFactory);
    }
}

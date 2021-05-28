package org.graylog2.lookup;

import org.graylog2.lookup.dto.CacheDto;
import org.graylog2.lookup.dto.DataAdapterDto;
import org.graylog2.lookup.dto.LookupTableDto;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

/**
 * Abstracts the configuration retrieval for {@link LookupTableService}.
 */
public interface LookupTableConfigService {
    /**
     * Returns the lookup table config for the given ID.
     *
     * @param id lookup table ID
     * @return Filled optional with the lookup table config or an empty optional if the lookup table doesn't exist
     */
    Optional<LookupTableDto> getTable(String id);

    /**
     * Returns all existing lookup table config objects.
     *
     * @return collection of lookup table config objects
     */
    Collection<LookupTableDto> loadAllTables();

    /**
     * Returns all lookup table config objets that use the given data adapter IDs.
     *
     * @param ids data adapter IDs
     * @return collection of lookup table config objects
     */
    Collection<LookupTableDto> findTablesForDataAdapterIds(Set<String> ids);

    /**
     * Returns all lookup table config objets that use the given cache IDs.
     *
     * @param ids cache IDs
     * @return collection of lookup table config objects
     */
    Collection<LookupTableDto> findTablesForCacheIds(Set<String> ids);

    /**
     * Returns all existing lookup data adapter config objects.
     *
     * @return collection of lookup data adapter config objects
     */
    Collection<DataAdapterDto> loadAllDataAdapters();

    /**
     * Returns all lookup data adapter config objects for the given IDs.
     *
     * @param ids data dapter IDs
     * @return collection of lookup data adapter config objects
     */
    Collection<DataAdapterDto> findDataAdaptersForIds(Set<String> ids);

    /**
     * Returns all existing lookup cache config objects.
     *
     * @return collection of lookup cache config objects
     */
    Collection<CacheDto> loadAllCaches();

    /**
     * Returns all lookup cache config objects for the given IDs.
     *
     * @param ids cache IDs
     * @return collection of lookup data adapter config objects
     */
    Collection<CacheDto> findCachesForIds(Set<String> ids);
}

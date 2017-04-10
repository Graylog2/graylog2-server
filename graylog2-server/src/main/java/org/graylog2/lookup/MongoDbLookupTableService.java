package org.graylog2.lookup;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;

import org.bson.types.ObjectId;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.graylog2.lookup.dto.CacheConfigurationDto;
import org.graylog2.lookup.dto.DataProviderDto;
import org.graylog2.lookup.dto.LookupTableDto;
import org.graylog2.plugin.lookup.LookupCache;
import org.graylog2.plugin.lookup.LookupDataAdapter;
import org.mongojack.DBQuery;
import org.mongojack.JacksonDBCollection;
import org.mongojack.WriteResult;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

public class MongoDbLookupTableService {

    private final JacksonDBCollection<LookupTableDto, ObjectId> lutDb;
    private final JacksonDBCollection<DataProviderDto, ObjectId> providersDb;
    private final JacksonDBCollection<CacheConfigurationDto, ObjectId> cachesDb;

    @Inject
    public MongoDbLookupTableService(MongoConnection mongoConnection,
                                     MongoJackObjectMapperProvider mapper) {

        lutDb = JacksonDBCollection.wrap(mongoConnection.getDatabase().getCollection("lut_tables"),
                LookupTableDto.class,
                ObjectId.class,
                mapper.get());

        providersDb = JacksonDBCollection.wrap(mongoConnection.getDatabase().getCollection("lut_dataproviders"),
                DataProviderDto.class,
                ObjectId.class,
                mapper.get());

        cachesDb = JacksonDBCollection.wrap(mongoConnection.getDatabase().getCollection("lut_caches"),
                CacheConfigurationDto.class,
                ObjectId.class,
                mapper.get());
    }

    public LookupTable save(LookupTable table) {
        WriteResult<LookupTableDto, ObjectId> save = lutDb.save(LookupTableDto.fromDomainObject(table));

        LookupTableDto savedObject = save.getSavedObject();
        return LookupTableDto.toDomainObject(savedObject);
    }

    public List<LookupTable> loadAllTables() {
        ImmutableList<LookupTableDto> lookupTableDtos = ImmutableList.copyOf((Iterator<? extends LookupTableDto>) lutDb.find());

        // resolve all references to other objects
        ImmutableSet.Builder<Object> cacheIds = ImmutableSet.builder();
        ImmutableSet.Builder<Object> dataProviderIds = ImmutableSet.builder();
        lookupTableDtos.forEach(dto -> {
            cacheIds.add(dto.cacheId());
            dataProviderIds.add(dto.dataProviderId());
        });

        ImmutableSet<CacheConfigurationDto> cacheConfigurationDtos = ImmutableSet.copyOf(cachesDb.find(DBQuery.in("_id", cacheIds.build())).iterator());
        ImmutableSet<DataProviderDto> dataProviderDtos = ImmutableSet.copyOf(providersDb.find(DBQuery.in("_id", dataProviderIds.build())).iterator());

        ImmutableMap<String, CacheConfigurationDto> cacheMap = Maps.uniqueIndex(cacheConfigurationDtos, CacheConfigurationDto::id);
        ImmutableMap<String, DataProviderDto> providerMap = Maps.uniqueIndex(dataProviderDtos, DataProviderDto::id);

        // finally fix up the references to be the internal domain objects
        // note that this does not load any data yet
        return lookupTableDtos.stream()
                .map(lookupTableDto -> lookupTableDto.toDomainObject(cacheMap, providerMap))
                .collect(Collectors.toList());
    }

    public LookupCache findCacheProvider(String name) {
        CacheConfigurationDto dto = cachesDb.findOne(DBQuery.is("name", name));
        return CacheConfigurationDto.toDomainObject(dto);
    }

    public LookupDataAdapter findDataProvider(String name) {
        return null;
    }
}

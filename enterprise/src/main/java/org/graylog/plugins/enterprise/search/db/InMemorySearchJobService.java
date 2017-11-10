package org.graylog.plugins.enterprise.search.db;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.bson.types.ObjectId;
import org.graylog.plugins.enterprise.search.Search;
import org.graylog.plugins.enterprise.search.SearchJob;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

// TODO dummy that only holds everything in memory for now
@Singleton
public class InMemorySearchJobService implements SearchJobService {

    private final Cache<String, SearchJob> cache;

    @Inject
    public InMemorySearchJobService() {
        cache = CacheBuilder.newBuilder()
                .expireAfterAccess(5, TimeUnit.MINUTES)
                .maximumSize(1000)
                .recordStats()
                .build();
    }

    @Override
    public SearchJob create(Search query) {
        final String id = new ObjectId().toHexString();
        final SearchJob searchJob = new SearchJob(id, query);
        cache.put(id, searchJob);
        return searchJob;
    }

    @Override
    public Optional<SearchJob> load(String id) {
        return Optional.ofNullable(cache.getIfPresent(id));
    }

    @Override
    public boolean delete(String id) {
        final Optional<SearchJob> load = load(id);
        if (load.isPresent()) {
            cache.invalidate(id);
            return true;
        }
        return false;
    }
}

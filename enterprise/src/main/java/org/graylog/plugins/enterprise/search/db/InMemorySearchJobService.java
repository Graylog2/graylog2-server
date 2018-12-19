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
    public SearchJob create(Search query, String owner) {
        final String id = new ObjectId().toHexString();
        final SearchJob searchJob = new SearchJob(id, query, owner);
        cache.put(id, searchJob);
        return searchJob;
    }

    @Override
    public Optional<SearchJob> load(String id, String owner) {
        final SearchJob searchJob = cache.getIfPresent(id);
        if (searchJob == null || !searchJob.getOwner().equals(owner)) {
            return Optional.empty();
        }
        return Optional.of(searchJob);
    }
}

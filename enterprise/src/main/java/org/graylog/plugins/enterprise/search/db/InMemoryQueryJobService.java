package org.graylog.plugins.enterprise.search.db;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.bson.types.ObjectId;
import org.graylog.plugins.enterprise.search.Query;
import org.graylog.plugins.enterprise.search.QueryJob;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

// TODO dummy that only holds everything in memory for now
@Singleton
public class InMemoryQueryJobService implements QueryJobService {

    private final Cache<String, QueryJob> cache;

    @Inject
    public InMemoryQueryJobService() {
        cache = CacheBuilder.newBuilder()
                .expireAfterAccess(5, TimeUnit.MINUTES)
                .maximumSize(1000)
                .recordStats()
                .build();
    }

    @Override
    public QueryJob create(Query query) {
        final String id = new ObjectId().toHexString();
        final QueryJob queryJob = new QueryJob(id, query);
        cache.put(id, queryJob);
        return queryJob;
    }

    @Override
    public Optional<QueryJob> load(String id) {
        return Optional.ofNullable(cache.getIfPresent(id));
    }

    @Override
    public boolean delete(String id) {
        final Optional<QueryJob> load = load(id);
        if (load.isPresent()) {
            cache.invalidate(id);
            return true;
        }
        return false;
    }
}

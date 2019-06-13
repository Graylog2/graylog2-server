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
package org.graylog.plugins.views.search.db;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.bson.types.ObjectId;
import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.SearchJob;
import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.SearchJob;

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

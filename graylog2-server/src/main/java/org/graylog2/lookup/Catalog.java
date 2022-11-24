package org.graylog2.lookup;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.graylog2.contentpacks.ContentPackService;

import javax.inject.Inject;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * TODO: convert into LookupTable
 */
public class Catalog {
    private final ContentPackService contentPackService;
    private final LoadingCache<String, String> titleCache;
    private final LoadingCache<String, String> typeCache;

    @Inject
    public Catalog(ContentPackService contentPackService) {
        this.contentPackService = contentPackService;
        this.titleCache = createTitleCache();
        this.typeCache = createTypeCache();
    }

    private LoadingCache<String, String> createTitleCache() {
        return CacheBuilder
                .newBuilder()
                .maximumSize(100)
                .expireAfterAccess(1, TimeUnit.SECONDS)
                .build(new CacheLoader<String, String>() {
                    @Override
                    public String load(String id) {
                        var catalog = contentPackService.getEntityExcerpts();
                        var excerpt = catalog.get(id);
                        if(excerpt != null) {
                            return excerpt.title();
                        } else {
                            return null;
                        }
                    }
                });
    }

    private LoadingCache<String, String> createTypeCache() {
        return CacheBuilder
                .newBuilder()
                .maximumSize(100)
                .expireAfterAccess(1, TimeUnit.SECONDS)
                .build(new CacheLoader<String, String>() {
                    @Override
                    public String load(String id) {
                        var catalog = contentPackService.getEntityExcerpts();
                        var excerpt = catalog.get(id);
                        if(excerpt != null) {
                            return excerpt.type().name();
                        } else {
                            return null;
                        }
                    }
                });
    }

    public String getTitle(final String id) {
        try {
            var title = titleCache.get(id);
            if(title != null) {
                return title;
            } else {
                return "Unknown entity: " + id;
            }
        } catch (ExecutionException cex) {
            return "Unknown entity: " + id;
        }
    }

    public String getType(final String id) {
        try {
            var title = typeCache.get(id);
            if(title != null) {
                return title;
            } else {
                return "Unknown entity: " + id;
            }
        } catch (ExecutionException cex) {
            return "Unknown entity: " + id;
        }
    }
}

package org.graylog2.shared.messageq;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.UncheckedExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Singleton
public class MessageQueueInstanceCache {
    private static final Logger LOG = LoggerFactory.getLogger(MessageQueueInstanceCache.class);

    private final Map<String, MessageQueueWriter.Factory> writerFactories;
    private final Map<String, MessageQueueReader.Factory> readerFactories;

    private final Cache<String, MessageQueueWriter> writerCache = CacheBuilder.newBuilder().build();
    private final Cache<String, MessageQueueReader> readerCache = CacheBuilder.newBuilder().build();

    @Inject
    public MessageQueueInstanceCache(Map<String, MessageQueueWriter.Factory> writerFactories,
                                     Map<String, MessageQueueReader.Factory> readerFactories) {
        this.writerFactories = writerFactories;
        this.readerFactories = readerFactories;
    }

    public MessageQueueWriter getWriter(String type, String name) {
        final String cacheKey = getCacheKey(type, name);

        try {
            return writerCache.get(cacheKey, () -> {
                LOG.info("Creating new <{}> writer instance named <{}>", type, name);
                final MessageQueueWriter.Factory factory = writerFactories.get(type);
                if (factory == null) {
                    throw new IllegalArgumentException("Message queue type <" + type + "> doesn't exist");
                }
                return factory.create(name);
            });
        } catch (ExecutionException e) {
            throw new UncheckedExecutionException("Couldn't create writer <" + type + "> instance with name <" + name + "> for cache key <"+ cacheKey + ">", e);
        }
    }

    public MessageQueueReader getReader(String type, String name) {
        final String cacheKey = getCacheKey(type, name);

        try {
            return readerCache.get(cacheKey, () -> {
                LOG.info("Creating new <{}> reader instance named <{}>", type, name);
                final MessageQueueReader.Factory factory = readerFactories.get(type);
                if (factory == null) {
                    throw new IllegalArgumentException("Message queue type <" + type + "> doesn't exist");
                }
                return factory.create(name);
            });
        } catch (ExecutionException e) {
            throw new UncheckedExecutionException("Couldn't create reader <" + type + "> instance with name <" + name + "> for cache key <"+ cacheKey + ">", e);
        }
    }

    private String getCacheKey(String type, String name) {
        return type + "-" + name;
    }
}

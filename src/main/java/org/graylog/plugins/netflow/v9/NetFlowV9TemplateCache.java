/*
 * Copyright 2013 Eediom Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.graylog.plugins.netflow.v9;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.requireNonNull;

public class NetFlowV9TemplateCache implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(NetFlowV9TemplateCache.class);

    private final Cache<Integer, NetFlowV9Template> cache;
    private final Path cachePath;
    private final ObjectMapper objectMapper;

    public NetFlowV9TemplateCache(long maximumSize,
                                  Path cachePath,
                                  long saveInterval,
                                  ScheduledExecutorService executorService,
                                  ObjectMapper objectMapper) {
        this(CacheBuilder.newBuilder()
                .maximumSize(maximumSize)
                .build(), cachePath, saveInterval, executorService, objectMapper);
    }

    private NetFlowV9TemplateCache(Cache<Integer, NetFlowV9Template> cache,
                                   Path cachePath,
                                   long saveInterval,
                                   ScheduledExecutorService executorService,
                                   ObjectMapper objectMapper) {
        this.cache = requireNonNull(cache, "cache");
        this.cachePath = requireNonNull(cachePath, "cachePath");
        this.objectMapper = requireNonNull(objectMapper, "objectMapper");

        loadCache();
        executorService.scheduleAtFixedRate(this, 5L, saveInterval, TimeUnit.SECONDS);
    }

    private void loadCache() {
        if (Files.exists(cachePath)) {
            final File cacheFile = cachePath.toFile();
            try {
                if (Files.size(cachePath) > 0) {
                    final Map<Integer, NetFlowV9Template> entries = objectMapper.readValue(cacheFile, new TypeReference<Map<Integer, NetFlowV9Template>>() {
                    });
                    cache.putAll(entries);
                }
            } catch (IOException e) {
                LOG.error("Couldn't load template cache from disk", e);
            }
        }
    }

    public void put(NetFlowV9Template template) {
        cache.put(template.templateId(), template);
    }

    @Nullable
    public NetFlowV9Template get(int id) {
        return cache.getIfPresent(id);
    }

    @Override
    public void run() {
        if (cache.size() != 0) {
            try {
                final File cacheFile = cachePath.toFile();
                objectMapper.writeValue(cacheFile, cache.asMap());
            } catch (IOException e) {
                LOG.error("Couldn't persist template cache to disk", e);
            }
        }
    }
}

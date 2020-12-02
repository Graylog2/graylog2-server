/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog.plugins.pipelineprocessor.db.mongodb;

import com.google.common.collect.ImmutableSet;
import com.mongodb.BasicDBObject;
import com.mongodb.MongoException;
import org.graylog.plugins.pipelineprocessor.db.PipelineDao;
import org.graylog.plugins.pipelineprocessor.db.PipelineService;
import org.graylog.plugins.pipelineprocessor.events.PipelinesChangedEvent;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.NotFoundException;
import org.graylog2.events.ClusterEventBus;
import org.mongojack.DBCursor;
import org.mongojack.DBQuery;
import org.mongojack.DBSort;
import org.mongojack.JacksonDBCollection;
import org.mongojack.WriteResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

public class MongoDbPipelineService implements PipelineService {
    private static final Logger log = LoggerFactory.getLogger(MongoDbPipelineService.class);

    private static final String COLLECTION = "pipeline_processor_pipelines";

    private final JacksonDBCollection<PipelineDao, String> dbCollection;
    private final ClusterEventBus clusterBus;

    @Inject
    public MongoDbPipelineService(MongoConnection mongoConnection,
                                  MongoJackObjectMapperProvider mapper,
                                  ClusterEventBus clusterBus) {
        this.dbCollection = JacksonDBCollection.wrap(
                mongoConnection.getDatabase().getCollection(COLLECTION),
                PipelineDao.class,
                String.class,
                mapper.get());
        this.clusterBus = clusterBus;
        dbCollection.createIndex(DBSort.asc("title"), new BasicDBObject("unique", true));
    }

    @Override
    public PipelineDao save(PipelineDao pipeline) {
        final WriteResult<PipelineDao, String> save = dbCollection.save(pipeline);
        final PipelineDao savedPipeline = save.getSavedObject();

        clusterBus.post(PipelinesChangedEvent.updatedPipelineId(savedPipeline.id()));

        return savedPipeline;
    }

    @Override
    public PipelineDao load(String id) throws NotFoundException {
        final PipelineDao pipeline = dbCollection.findOneById(id);
        if (pipeline == null) {
            throw new NotFoundException("No pipeline with id " + id);
        }
        return pipeline;
    }

    @Override
    public PipelineDao loadByName(String name) throws NotFoundException {
        final DBQuery.Query query = DBQuery.is("title", name);
        final PipelineDao pipeline = dbCollection.findOne(query);
        if (pipeline == null) {
            throw new NotFoundException("No pipeline with name " + name);
        }
        return pipeline;
    }

    @Override
    public Collection<PipelineDao> loadAll() {
        try (DBCursor<PipelineDao> daos = dbCollection.find()) {
            return ImmutableSet.copyOf((Iterator<PipelineDao>) daos);
        } catch (MongoException e) {
            log.error("Unable to load pipelines", e);
            return Collections.emptySet();
        }
    }

    @Override
    public void delete(String id) {
        dbCollection.removeById(id);
        clusterBus.post(PipelinesChangedEvent.deletedPipelineId(id));
    }
}

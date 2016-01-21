/**
 * This file is part of Graylog Pipeline Processor.
 *
 * Graylog Pipeline Processor is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog Pipeline Processor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog Pipeline Processor.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog.plugins.pipelineprocessor.db;

import com.google.common.collect.Sets;
import com.mongodb.MongoException;
import org.graylog.plugins.pipelineprocessor.rest.PipelineSource;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.NotFoundException;
import org.mongojack.DBCursor;
import org.mongojack.JacksonDBCollection;
import org.mongojack.WriteResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Collections;

public class PipelineSourceService {
    private static final Logger log = LoggerFactory.getLogger(PipelineSourceService.class);

    public static final String COLLECTION = "pipeline_processor_pipelines";

    private final JacksonDBCollection<PipelineSource, String> dbCollection;

    @Inject
    public PipelineSourceService(MongoConnection mongoConnection, MongoJackObjectMapperProvider mapper) {
        dbCollection = JacksonDBCollection.wrap(
                mongoConnection.getDatabase().getCollection(COLLECTION),
                PipelineSource.class,
                String.class,
                mapper.get());
    }

    public PipelineSource save(PipelineSource pipeline) {
        final WriteResult<PipelineSource, String> save = dbCollection.save(pipeline);
        return save.getSavedObject();
    }

    public PipelineSource load(String id) throws NotFoundException {
        final PipelineSource pipeline = dbCollection.findOneById(id);
        if (pipeline == null) {
            throw new NotFoundException("No pipeline with id " + id);
        }
        return pipeline;
    }

    public Collection<PipelineSource> loadAll() {
        try {
            final DBCursor<PipelineSource> pipelineSources = dbCollection.find();
            return Sets.newHashSet(pipelineSources.iterator());
        } catch (MongoException e) {
            log.error("Unable to load pipelines", e);
            return Collections.emptySet();
        }
    }

    public void delete(String id) {
        dbCollection.removeById(id);
    }
}

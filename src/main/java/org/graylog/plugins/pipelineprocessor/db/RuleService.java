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
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.NotFoundException;
import org.mongojack.DBCursor;
import org.mongojack.DBSort;
import org.mongojack.JacksonDBCollection;
import org.mongojack.WriteResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Collections;

public class RuleService {
    private static final Logger log = LoggerFactory.getLogger(RuleService.class);

    public static final String COLLECTION = "pipeline_processor_rules";

    private final JacksonDBCollection<RuleDao, String> dbCollection;

    @Inject
    public RuleService(MongoConnection mongoConnection, MongoJackObjectMapperProvider mapper) {
        dbCollection = JacksonDBCollection.wrap(
                mongoConnection.getDatabase().getCollection(COLLECTION),
                RuleDao.class,
                String.class,
                mapper.get());
        dbCollection.createIndex(DBSort.asc("title"));
    }

    public RuleDao save(RuleDao rule) {
        final WriteResult<RuleDao, String> save = dbCollection.save(rule);
        return save.getSavedObject();
    }

    public RuleDao load(String id) throws NotFoundException {
        final RuleDao rule = dbCollection.findOneById(id);
        if (rule == null) {
            throw new NotFoundException("No rule with id " + id);
        }
        return rule;
    }

    public Collection<RuleDao> loadAll() {
        try {
            final DBCursor<RuleDao> ruleDaos = dbCollection.find();
            return Sets.newHashSet(ruleDaos.iterator());
        } catch (MongoException e) {
            log.error("Unable to load processing rules", e);
            return Collections.emptySet();
        }
    }

    public void delete(String id) {
        final WriteResult<RuleDao, String> result = dbCollection.removeById(id);
        if (result.getN() != 1) {
            log.error("Unable to delete rule {}", id);
        }
    }
}

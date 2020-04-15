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
package org.graylog2.inputs.persistence;

import com.google.common.eventbus.EventBus;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import org.apache.shiro.event.Subscribe;
import org.bson.types.ObjectId;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.graylog2.rest.models.system.inputs.responses.InputDeleted;
import org.mongojack.JacksonDBCollection;
import org.mongojack.WriteResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Optional;

/**
 * This is a simple wrapper around MongoDB to allow storage of state data for stateful Inputs.
 *
 * Inputs using this service are responsible for defining their own model for InputStatusRecord.inputStateData
 */
@Singleton
public class MongoInputStatusService implements InputStatusService {
    private static final Logger LOG = LoggerFactory.getLogger(MongoInputStatusService.class);

    public static final String COLLECTION_NAME = "input_status";

    private final JacksonDBCollection<InputStatusRecord, ObjectId> statusCollection;

    @Inject
    public MongoInputStatusService(MongoConnection mongoConnection,
                              MongoJackObjectMapperProvider objectMapperProvider,
                              EventBus eventBus) {
        DB mongoDatabase = mongoConnection.getDatabase();
        DBCollection collection = mongoDatabase.getCollection(COLLECTION_NAME);

        eventBus.register(this);

        statusCollection = JacksonDBCollection.wrap(
                collection,
                InputStatusRecord.class,
                ObjectId.class,
                objectMapperProvider.get());
    }

    @Override
    public Optional<InputStatusRecord> get(final String inputId) {
        return Optional.ofNullable(statusCollection.findOneById(new ObjectId(inputId)));
    }

    @Override
    public InputStatusRecord save(InputStatusRecord statusRecord) {
        WriteResult save = statusCollection.save(statusRecord);
        return (InputStatusRecord) save.getSavedObject();
    }

    @Override
    public int delete(String inputId) {
        WriteResult delete = statusCollection.removeById(new ObjectId(inputId));
        return delete.getN();
    }

    /**
     * Clean up MongoDB records when Inputs are deleted
     *
     * At the moment, Graylog uses the InputDeleted event both when an Input is stopped
     * and when it is deleted.
     *
     * @param event ID of the input being deleted
     */
    @Subscribe
    public void handleInputDeleted(InputDeleted event) {
        LOG.debug("Input Deleted event received for Input [{}]", event.id());
        // TODO: Pending issue #7812
        // delete(event.id());
    }
}

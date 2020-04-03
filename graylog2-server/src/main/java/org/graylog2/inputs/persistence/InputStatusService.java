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
public class InputStatusService {
    private static final Logger LOG = LoggerFactory.getLogger(InputStatusService.class);

    public static final String COLLECTION_NAME = "input_status";

    private final JacksonDBCollection<InputStatusRecord, ObjectId> statusCollection;

    @Inject
    public InputStatusService(MongoConnection mongoConnection,
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

    /**
     * Get the status record for an Input from the DB.
     *
     * @param inputId ID of the input whose status you want to get
     * @return The InputStatusRecord for the given Input ID if it exists; otherwise an empty Optional.
     */
    public Optional<InputStatusRecord> get(final String inputId) {
        return Optional.ofNullable(statusCollection.findOneById(new ObjectId(inputId)));
    }

    /**
     * Save the status record for an Input status to the DB.
     *
     * @param statusRecord The Input status record to save
     * @return A copy of the saved object
     */
    public InputStatusRecord save(InputStatusRecord statusRecord) {
        WriteResult save = statusCollection.save(statusRecord);
        return (InputStatusRecord) save.getSavedObject();
    }

    /**
     * Remove the status record from the DB for a given Input
     *
     * @param inputId ID of the input whose status you want to delete
     * @return The count of deleted objects (should be 0 or 1)
     */
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

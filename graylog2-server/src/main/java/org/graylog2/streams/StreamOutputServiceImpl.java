package org.graylog2.streams;

import com.beust.jcommander.internal.Lists;
import com.google.common.collect.ImmutableSet;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import org.bson.types.ObjectId;
import org.graylog2.database.*;
import org.graylog2.plugin.database.Persisted;
import org.graylog2.plugin.database.validators.Validator;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.plugin.streams.StreamOutput;
import org.graylog2.streams.outputs.CreateStreamOutputRequest;
import org.joda.time.DateTime;
import org.mongojack.DBCursor;
import org.mongojack.DBQuery;
import org.mongojack.JacksonDBCollection;

import javax.inject.Inject;
import java.util.*;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class StreamOutputServiceImpl implements StreamOutputService {
    final private JacksonDBCollection<StreamOutputImpl, String> coll;
    @Inject
    public StreamOutputServiceImpl(MongoConnection mongoConnection) {
        final String collectionName = StreamOutputImpl.class.getAnnotation(CollectionName.class).value();
        final DBCollection dbCollection = mongoConnection.getDatabase().getCollection(collectionName);
        coll = JacksonDBCollection.wrap(dbCollection, StreamOutputImpl.class, String.class);
    }

    @Override
    public StreamOutput load(String streamOutputId) throws NotFoundException {
        final StreamOutput result = coll.findOneById(streamOutputId);
        if (result == null)
            throw new NotFoundException();
        return result;
    }

    protected List<StreamOutput> toInterface(List<StreamOutputImpl> input) {
        List<StreamOutput> result = new ArrayList<>();
        for (StreamOutput streamOutput : input)
            result.add(streamOutput);

        return result;
    }

    protected List<StreamOutput> toInterface(DBCursor<StreamOutputImpl> input) {
        return toInterface(input.toArray());
    }

    protected List<StreamOutput> loadAll(DBObject additionalQueryOpts) {
        return toInterface(coll.find(additionalQueryOpts).toArray());
    }

    @Override
    public Set<StreamOutput> loadAll() {
        return ImmutableSet.copyOf(loadAll(new BasicDBObject()));
    }

    @Override
    public Set<StreamOutput> loadAllForStream(String streamId) {
        return ImmutableSet.copyOf(toInterface(coll.find().in("streams", streamId)));
    }

    @Override
    public Set<StreamOutput> loadAllForStream(Stream stream) {
        return loadAllForStream(stream.getId());
    }

    @Override
    public StreamOutput create(StreamOutput request) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public StreamOutput create(final String streamId, final CreateStreamOutputRequest request) {
        /*final Set<ObjectId> streams = new HashSet<ObjectId>() {{
            add(new ObjectId(streamId));
        }};
        Map<String, Object> fields = new HashMap<String, Object>() {{
            put("stream_id", streams);
            put("title", request.title);
            put("type", request.type);
            put("configuration", request.configuration);
            put("creator_user_id", request.creatorUserId);
            put("created_at", DateTime.now());
        }};

        return new StreamOutputImpl(fields);*/
        return null;
    }

    @Override
    public <T extends Persisted> int destroy(T model) {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public <T extends Persisted> int destroyAll(Class<T> modelClass) {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public <T extends Persisted> String save(T model) throws ValidationException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public <T extends Persisted> String saveWithoutValidation(T model) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public <T extends Persisted> boolean validate(T model, Map<String, Object> fields) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public <T extends Persisted> boolean validate(T model) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean validate(Map<String, Validator> validators, Map<String, Object> fields) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }
}

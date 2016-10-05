package org.graylog2.streams;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.CollectionName;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.NotFoundException;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.plugin.streams.StreamRule;
import org.graylog2.rest.resources.streams.rules.requests.CreateStreamRuleRequest;
import org.mongojack.Aggregation;
import org.mongojack.DBQuery;
import org.mongojack.JacksonDBCollection;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;

public class StreamRuleServiceImpl implements StreamRuleService {
    private final JacksonDBCollection<StreamRuleImpl, String> coll;

    @Inject
    public StreamRuleServiceImpl(MongoConnection mongoConnection,
                                 MongoJackObjectMapperProvider mapperProvider) {
        final String collectionName = StreamRuleImpl.class.getAnnotation(CollectionName.class).value();
        final DBCollection dbCollection = mongoConnection.getDatabase().getCollection(collectionName);
        this.coll = JacksonDBCollection.wrap(dbCollection, StreamRuleImpl.class, String.class, mapperProvider.get());
    }

    @Override
    public StreamRule load(String id) throws NotFoundException {
        return this.coll.findOneById(id);
    }

    @Override
    public List<StreamRule> loadForStream(Stream stream) throws NotFoundException {
        return loadForStreamId(stream.getId());
    }

    @Override
    public StreamRule create(String streamId, CreateStreamRuleRequest request) {
        return StreamRuleImpl.create(streamId, request);
    }

    @Override
    public StreamRule.Builder builder() {
        return StreamRuleImpl.builder();
    }

    @Override
    public List<StreamRule> loadForStreamId(String streamId) throws NotFoundException {
        return this.coll.find(DBQuery.is(StreamRuleImpl.FIELD_STREAM_ID, streamId)).toArray()
            .stream()
            .collect(Collectors.toList());
    }

    @Override
    public long totalStreamRuleCount() {
        return this.coll.count();
    }

    @Override
    public long streamRuleCount(String streamId) {
        return this.coll.count(new BasicDBObject(StreamRuleImpl.FIELD_STREAM_ID, streamId));
    }

    @Override
    public Map<String, Long> streamRuleCountByStream() {
        final List<Map> result = this.coll.aggregate(Aggregation.group(StreamRuleImpl.FIELD_STREAM_ID).set("value", Aggregation.Group.count()), Map.class).results();
        return result.stream().collect(Collectors.toMap((map) -> map.get("_id").toString(), (map) -> Long.parseLong(map.get("value").toString())));
    }

    @Override
    public String save(StreamRule streamRule) {
        return this.coll.save(implOrFail(streamRule)).getSavedId();
    }

    @Override
    public void destroy(StreamRule streamRule) {
        this.coll.removeById(streamRule.getId());
    }

    @Override
    public StreamRule clone(StreamRule streamRule) {
        return StreamRuleImpl.create(null,
            streamRule.getType().toInteger(),
            streamRule.getField(),
            streamRule.getValue(),
            streamRule.getInverted(),
            streamRule.getStreamId(),
            streamRule.getContentPack(),
            streamRule.getDescription());
    }

    private StreamRuleImpl implOrFail(StreamRule streamRule) {
        checkArgument(streamRule instanceof StreamRuleImpl, "Supplied stream rule must be of implementation type " + StreamRuleImpl.class + ", not " + streamRule.getClass());
        return (StreamRuleImpl)streamRule;
    }
}

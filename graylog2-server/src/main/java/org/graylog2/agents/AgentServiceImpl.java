package org.graylog2.agents;

import com.google.common.collect.Lists;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.CollectionName;
import org.graylog2.database.MongoConnection;
import org.mongojack.DBCursor;
import org.mongojack.DBQuery;
import org.mongojack.JacksonDBCollection;
import org.mongojack.WriteResult;

import javax.inject.Inject;
import java.util.List;

public class AgentServiceImpl implements AgentService {

    private final JacksonDBCollection<AgentImpl, String> coll;

    @Inject
    public AgentServiceImpl(MongoConnection mongoConnection,
                            MongoJackObjectMapperProvider mapperProvider) {
        final String collectionName = AgentImpl.class.getAnnotation(CollectionName.class).value();
        final DBCollection dbCollection = mongoConnection.getDatabase().getCollection(collectionName);
        this.coll = JacksonDBCollection.wrap(dbCollection, AgentImpl.class, String.class, mapperProvider.get());
    }

    @Override
    public long count() {
        return coll.count();
    }

    @Override
    public Agent save(Agent agent) {
        if (agent instanceof AgentImpl) {
            final AgentImpl agentImpl = (AgentImpl)agent;
            final WriteResult<AgentImpl, String> result = coll.save(agentImpl);
            final DBObject dbObject = result.getDbObject();
            return result.getSavedObject();
        } else
            throw new IllegalArgumentException("Specified object is not of correct implementation type (" + agent.getClass() + ")!");
    }

    @Override
    public List<Agent> all() {
        return toAbstractListType(coll.find());
    }

    @Override
    public Agent findById(String id) {
        return coll.findOne(DBQuery.is("id", id));
    }

    @Override
    public List<Agent> findByNodeId(String nodeId) {
        return toAbstractListType(coll.find(DBQuery.is("node_id", nodeId)));
    }

    @Override
    public int destroy(Agent agent) {
        return coll.remove(DBQuery.is("id", agent.getId())).getN();
    }

    private List<Agent> toAbstractListType(DBCursor<AgentImpl> agents) {
        return toAbstractListType(agents.toArray());
    }

    private List<Agent> toAbstractListType(List<AgentImpl> agents) {
        final List<Agent> result = Lists.newArrayListWithCapacity(agents.size());
        result.addAll(agents);

        return result;
    }
}

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
package org.graylog2.agents;

import com.google.common.collect.Lists;
import com.google.common.primitives.Ints;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.CollectionName;
import org.graylog2.database.MongoConnection;
import org.graylog2.rest.models.agent.requests.AgentRegistrationRequest;
import org.joda.time.DateTime;
import org.mongojack.DBCursor;
import org.mongojack.DBQuery;
import org.mongojack.JacksonDBCollection;
import org.mongojack.WriteResult;

import javax.inject.Inject;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class AgentServiceImpl implements AgentService {

    private final JacksonDBCollection<AgentImpl, String> coll;
    private final Validator validator;

    @Inject
    public AgentServiceImpl(MongoConnection mongoConnection,
                            MongoJackObjectMapperProvider mapperProvider,
                            Validator validator) {
        this.validator = validator;
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
            final Set<ConstraintViolation<AgentImpl>> violations = validator.validate(agentImpl);
            if (violations.isEmpty()) {
                return coll.findAndModify(DBQuery.is("id", agent.getId()), new BasicDBObject(), new BasicDBObject(), false, agentImpl, true, true);
            } else {
                throw new IllegalArgumentException("Specified object failed validation: " + violations);
            }
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

    @Override
    public int destroyExpired(int time, TimeUnit unit) {
        int count = 0;
        final DateTime threshold = DateTime.now().minusSeconds(Ints.checkedCast(unit.toSeconds(time)));
        for (Agent agent : all())
            if (agent.getLastSeen().isBefore(threshold))
                count += destroy(agent);

        return count;
    }

    @Override
    public Agent fromRequest(String agentId, AgentRegistrationRequest request, String agentVersion) {
        return AgentImpl.create(agentId, request.nodeId(), agentVersion, AgentNodeDetails.create(request.nodeDetails().operatingSystem()), DateTime.now());
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

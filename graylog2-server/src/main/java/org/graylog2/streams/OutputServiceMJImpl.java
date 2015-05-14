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
package org.graylog2.streams;

import com.google.common.collect.Sets;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import org.bson.types.ObjectId;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.CollectionName;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.NotFoundException;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.plugin.streams.Output;
import org.graylog2.rest.models.streams.outputs.requests.CreateOutputRequest;
import org.mongojack.DBQuery;
import org.mongojack.DBUpdate;
import org.mongojack.JacksonDBCollection;
import org.mongojack.WriteResult;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class OutputServiceMJImpl implements OutputService {
    private final JacksonDBCollection<OutputAVImpl, String> coll;
    private final DBCollection dbCollection;
    private final StreamService streamService;

    @Inject
    public OutputServiceMJImpl(MongoConnection mongoConnection,
                               MongoJackObjectMapperProvider mapperProvider,
                               StreamService streamService) {
        this.streamService = streamService;
        final String collectionName = OutputAVImpl.class.getAnnotation(CollectionName.class).value();
        this.dbCollection = mongoConnection.getDatabase().getCollection(collectionName);
        this.coll = JacksonDBCollection.wrap(dbCollection, OutputAVImpl.class, String.class, mapperProvider.get());
    }

    @Override
    public Output load(String streamOutputId) throws NotFoundException {
        return coll.findOneById(streamOutputId);
    }

    @Override
    public Set<Output> loadAll() {
        return toAbstractSetType(coll.find().toArray());
    }

    private Set<Output> toAbstractSetType(List<OutputAVImpl> outputs) {
        final Set<Output> result = Sets.newHashSet();
        result.addAll(outputs);

        return result;
    }

    @Override
    public Output create(Output request) throws ValidationException {
        final OutputAVImpl outputImpl = implOrFail(request);
        final WriteResult<OutputAVImpl, String> writeResult = coll.save(outputImpl);

        return writeResult.getSavedObject();
    }

    @Override
    public Output create(CreateOutputRequest request, String userId) throws ValidationException {
        return create(OutputAVImpl.create(new ObjectId().toHexString(), request.title(), request.type(), userId, request.configuration(),
                Tools.iso8601().toDate(), request.contentPack()));
    }

    @Override
    public void destroy(Output model) throws NotFoundException {
        coll.removeById(model.getId());
        streamService.removeOutputFromAllStreams(model);
    }

    @Override
    public Output update(String id, Map<String, Object> deltas) {
        DBUpdate.Builder update = new DBUpdate.Builder();
        for (Map.Entry<String, Object> fields : deltas.entrySet())
            update = update.set(fields.getKey(), fields.getValue());

        return coll.findAndModify(DBQuery.is("_id", id), update);
    }

    @Override
    public long count() {
        return coll.count();
    }

    @Override
    public Map<String, Long> countByType() {
        final DBCursor outputTypes = dbCollection.find(null, new BasicDBObject(OutputImpl.FIELD_TYPE, 1));

        final Map<String, Long> outputsCountByType = new HashMap<>(outputTypes.count());
        for (DBObject outputType : outputTypes) {
            final String type = (String) outputType.get(OutputImpl.FIELD_TYPE);
            if (type != null) {
                final Long oldValue = outputsCountByType.get(type);
                final Long newValue = (oldValue == null) ? 1 : oldValue + 1;
                outputsCountByType.put(type, newValue);
            }
        }

        return outputsCountByType;
    }

    private OutputAVImpl implOrFail(Output output) {
        final OutputAVImpl outputImpl;
        if (output instanceof OutputAVImpl) {
            outputImpl = (OutputAVImpl) output;
            return outputImpl;
        } else {
            throw new IllegalArgumentException("Supplied output must be of implementation type OutputImpl, not " + output.getClass());
        }
    }
}

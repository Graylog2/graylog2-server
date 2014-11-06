/**
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.streams;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.bson.types.ObjectId;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.NotFoundException;
import org.graylog2.database.PersistedServiceImpl;
import org.graylog2.database.ValidationException;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.streams.Output;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.streams.outputs.CreateOutputRequest;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class OutputServiceImpl extends PersistedServiceImpl implements OutputService {
    private final StreamService streamService;

    @Inject
    public OutputServiceImpl(MongoConnection mongoConnection,
                             StreamService streamService) {
        super(mongoConnection);
        this.streamService = streamService;
    }

    @Override
    public Output load(String streamOutputId) throws NotFoundException {
        DBObject o = get(OutputImpl.class, streamOutputId);

        if (o == null) {
            throw new NotFoundException("Output <" + streamOutputId + "> not found!");
        }

        return new OutputImpl((ObjectId) o.get("_id"), o.toMap());
    }

    @Override
    public Set<Output> loadAll() {
        return loadAll(new HashMap<String, Object>());
    }

    protected Set<Output> loadAll(Map<String, Object> additionalQueryOpts) {
        Set<Output> outputs = new HashSet<>();

        DBObject query = new BasicDBObject();

        // putAll() is not working with BasicDBObject.
        for (Map.Entry<String, Object> o : additionalQueryOpts.entrySet()) {
            query.put(o.getKey(), o.getValue());
        }

        List<DBObject> results = query(OutputImpl.class, query);
        for (DBObject o : results)
            outputs.add(new OutputImpl((ObjectId) o.get("_id"), o.toMap()));

        return outputs;
    }

    @Override
    public Set<Output> loadForStream(Stream stream) {
        return stream.getOutputs();
    }

    @Override
    public Output create(Output request) throws ValidationException {
        final String id = save(request);
        if (request instanceof OutputImpl) {
            OutputImpl impl = OutputImpl.class.cast(request);
            impl.setId(id);
        }
        return request;
    }

    @Override
    public Output create(CreateOutputRequest request, String userId) throws ValidationException {
        return create(new OutputImpl(request.title, request.type, request.configuration,
                Tools.iso8601().toDate(), userId, request.contentPack));
    }

    @Override
    public void destroy(Output output) throws NotFoundException {
        streamService.removeOutputFromAllStreams(output);
        super.destroy(output);
    }
}

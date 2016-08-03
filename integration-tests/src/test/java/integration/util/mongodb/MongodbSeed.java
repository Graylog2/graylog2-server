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
package integration.util.mongodb;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import integration.IntegrationTestsConfig;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;

public class MongodbSeed {
    private final MongoClient mongoClient;
    private final DB mongoDatabase;

    public MongodbSeed(String dbName) throws UnknownHostException {
        mongoClient = new MongoClient(
                IntegrationTestsConfig.getMongodbHost(),
                IntegrationTestsConfig.getMongodbPort());
        mongoDatabase = mongoClient.getDB(dbName);
        mongoDatabase.dropDatabase();
    }

    private Map<String, List<DBObject>> parseDatabaseDump(URI seedUrl) throws IOException {
        final DumpReader dumpReader;
        if (seedUrl.getPath().endsWith(".json")) {
            dumpReader = new JsonReader(seedUrl);
        } else {
            dumpReader = new BsonReader(seedUrl);
        }
        return dumpReader.toMap();
    }

    private Map<String, List<DBObject>> updateNodeIdFirstNode(Map<String, List<DBObject>> collections, String nodeId) {
        final List<DBObject> nodes = collections.get("nodes");

        if (nodes == null || nodes.isEmpty())
            return collections;

        final DBObject firstNode = nodes.get(0);
        firstNode.put("node_id", nodeId);
        nodes.set(0, firstNode);

        collections.remove("nodes");
        collections.put("nodes", nodes);

        return collections;
    }

    private Map<String, List<DBObject>> updateNodeIdInputs(Map<String, List<DBObject>> collections, String nodeId) {
        List<DBObject> inputs = collections.get("inputs");

        if (inputs == null) {
            return collections;
        }

        for (DBObject input : inputs){
            input.put("node_id", nodeId);
        }

        collections.remove("inputs");
        collections.put("inputs", inputs);

        return collections;
    }

    public void loadDataset(URL dbPath, String nodeId) throws IOException, URISyntaxException {
        Map<String, List<DBObject>> collections = parseDatabaseDump(dbPath.toURI());
        collections = updateNodeIdFirstNode(collections, nodeId);
        collections = updateNodeIdInputs(collections, nodeId);

        for (Map.Entry<String, List<DBObject>> collection : collections.entrySet()) {
            final String collectionName = collection.getKey();
            if (mongoDatabase.getCollection(collectionName) == null) {
                mongoDatabase.createCollection(collectionName, new BasicDBObject());
            }
            final DBCollection mongoCollection = mongoDatabase.getCollection(collectionName);

            if (!collection.getValue().isEmpty()) {
                mongoCollection.insert(collection.getValue());
            }
        }
    }
}

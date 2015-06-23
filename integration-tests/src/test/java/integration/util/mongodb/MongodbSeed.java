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

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MongodbSeed {
    private final MongoClient mongoClient;
    private final String dbName;

    public MongodbSeed(String dbName) {
        this.dbName = dbName;
        mongoClient = new MongoClient(
                URI.create(System.getProperty("gl2.baseuri", "http://localhost")).getHost(),
                Integer.parseInt(System.getProperty("mongodb.port", "27017")));
        mongoClient.dropDatabase(dbName);
    }

    private Map<String, List<Document>> parseDatabaseDump(URL seedUrl) throws IOException {
        final DumpReader dumpReader;
        if (seedUrl.getPath().endsWith(".json")) {
            dumpReader = new JsonReader(seedUrl);
        } else {
            dumpReader = new BsonReader(seedUrl);
        }
        return dumpReader.toMap();
    }

    private Map<String, List<Document>> updateNodeIdFirstNode(Map<String, List<Document>> collections, String nodeId) {
        final List<Document> nodes = collections.get("nodes");

        if (nodes == null || nodes.isEmpty())
            return collections;

        Document firstNode = nodes.get(0);
        firstNode.put("node_id", nodeId);
        nodes.set(0, firstNode);

        collections.remove("nodes");
        collections.put("nodes", nodes);

        return collections;
    }

    private Map<String, List<Document>> updateNodeIdInputs(Map<String, List<Document>> collections, String nodeId) {
        List<Document> inputs = collections.get("inputs");

        if (inputs == null) {
            return collections;
        }

        for (Document input : inputs){
            input.put("node_id", nodeId);
        }

        collections.remove("inputs");
        collections.put("inputs", inputs);

        return collections;
    }

    public void loadDataset(URL dbPath, String nodeId) throws IOException {
        Map<String, List<Document>> collections = parseDatabaseDump(dbPath);
        collections = updateNodeIdFirstNode(collections, nodeId);
        collections = updateNodeIdInputs(collections, nodeId);

        final MongoDatabase mongoDatabase = mongoClient.getDatabase(dbName);

        for (Map.Entry<String, List<Document>> collection : collections.entrySet()) {
            final String collectionName = collection.getKey();
            if (mongoDatabase.getCollection(collectionName) == null) {
                mongoDatabase.createCollection(collectionName);
            }
            final MongoCollection<Document> mongoCollection = mongoDatabase.getCollection(collectionName);

            if (!collection.getValue().isEmpty()) {
                mongoCollection.insertMany(collection.getValue());
            }
        }
    }
}

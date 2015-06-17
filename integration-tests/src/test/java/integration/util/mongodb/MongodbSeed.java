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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.apache.commons.io.FilenameUtils;
import org.bson.BSONDecoder;
import org.bson.BSONObject;
import org.bson.BasicBSONDecoder;
import org.bson.Document;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MongodbSeed {
    public List<Document> readBsonFile(String filename){
        Path filePath = Paths.get(filename);
        List<Document> dataset = new ArrayList<>();

        try {
            ByteArrayInputStream fileBytes = new ByteArrayInputStream(Files.readAllBytes(filePath));
            BSONDecoder decoder = new BasicBSONDecoder();
            BSONObject obj;

            while((obj = decoder.readObject(fileBytes)) != null) {
                if(!obj.toString().trim().isEmpty()) {
                    Document mongoDocument = new Document();
                    mongoDocument.putAll(obj.toMap());
                    dataset.add(mongoDocument);
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException("Can not open BSON input file.", e);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw new RuntimeException("Can not parse BSON data.", e);
        } catch (IOException e) {
            //EOF
        }

        return dataset;
    }

    public Map<String, List<Document>> parseDatabaseDump(String dbPath){
        Map<String, List<Document>> collections = new HashMap<>();

        URL seedUrl = Thread.currentThread().getContextClassLoader().getResource("integration/seeds/mongodb/" + dbPath);
        File dir = new File(seedUrl.getPath());
        File[] collectionListing = dir.listFiles();

        if (collectionListing != null) {
            for (File collection : collectionListing) {
                if (collection.getName().endsWith(".bson") && !collection.getName().startsWith("system.indexes.")) {
                    List<Document> collectionData = readBsonFile(collection.getAbsolutePath());
                    collections.put(FilenameUtils.removeExtension(collection.getName()), collectionData);
                }
            }
        }


        return collections;
    }

    public Map<String, List<Document>> updateNodeIdFirstNode(Map<String, List<Document>> collections, String nodeId) {
        List<Document> nodes = new ArrayList<>();

        for (Map.Entry<String, List<Document>> collection : collections.entrySet()) {
            if(collection.getKey().equals("nodes")) {
                nodes = collection.getValue();
            }
        }

        Document firstNode = nodes.get(0);
        firstNode.put("node_id", nodeId);
        nodes.set(0, firstNode);

        collections.remove("nodes");
        collections.put("nodes", nodes);

        return collections;
    }

    public Map<String, List<Document>> updateNodeIdInputs(Map<String, List<Document>> collections, String nodeId) {
        List<Document> inputs = new ArrayList<>();

        for (Map.Entry<String, List<Document>> collection : collections.entrySet()) {
            if(collection.getKey().equals("inputs")) {
                for (Document input : collection.getValue()){
                    input.remove("node_id");
                    input.put("node_id", nodeId);
                    inputs.add(input);
                }

            }
        }

        collections.remove("inputs");
        collections.put("inputs", inputs);

        return collections;
    }

    public void loadDataset(String dbPath, String dbName, String nodeId){
        Map<String, List<Document>> collections = parseDatabaseDump(dbPath);
        collections = updateNodeIdFirstNode(collections, nodeId);
        collections = updateNodeIdInputs(collections, nodeId);

        MongoClient mongoClient = new MongoClient(
                URI.create(System.getProperty("gl2.baseuri", "http://localhost")).getHost(),
                Integer.parseInt(System.getProperty("mongodb.port", "27017")));

        mongoClient.dropDatabase(dbName);
        MongoDatabase mongoDatabase = mongoClient.getDatabase(dbName);

        for (Map.Entry<String, List<Document>> collection : collections.entrySet()) {
            String collectionName = collection.getKey();
            mongoDatabase.createCollection(collectionName);
            MongoCollection<Document> mongoCollection = mongoDatabase.getCollection(collection.getKey());
            for (Document document : collection.getValue()) {
                mongoCollection.insertOne(document);
            }
        }
    }
}

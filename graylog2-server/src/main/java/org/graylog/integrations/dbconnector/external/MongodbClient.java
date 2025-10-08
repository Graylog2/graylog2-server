/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog.integrations.dbconnector.external;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.mongodb.client.model.Filters.gt;
import static org.graylog.integrations.dbconnector.DBConnectorProperty.BATCH_SIZE;
import static org.graylog.integrations.dbconnector.DBConnectorProperty.LIMIT_RECORDS;

public class MongodbClient implements DBConnectorClient {

    private MongoClient client;
    ObjectMapper dtoMapper = new ObjectMapper();

    @Override
    public void getConnection(String connectionString) {
        ConnectionString cs = new ConnectionString(connectionString);
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(cs)
                .build();
        client = MongoClients.create(settings);
    }

    public JsonNode validateConnection(DBConnectorTransferObject dto) {
        MongoDatabase database = client.getDatabase(dto.databaseName());
        MongoCollection<Document> collection = database.getCollection(dto.mongoCollectionName());
        FindIterable<Document> fi = collection.find().limit(LIMIT_RECORDS);
        List<JsonNode> logList = StreamSupport.stream(fi.spliterator(), false)
                .map(doc -> dtoMapper.convertValue(doc, JsonNode.class)).collect(Collectors.toList());
        client.close();
        return dtoMapper.convertValue(logList, JsonNode.class);
    }

    public List<String> fetchLogs(DBConnectorTransferObject dto) {
        MongoDatabase database = client.getDatabase(dto.databaseName());
        MongoCollection<Document> collection = database.getCollection(dto.mongoCollectionName());
        FindIterable<Document> iterDoc;
        List<String> listLogs = new ArrayList<>();
        int offset = 0;
        long count = collection.countDocuments(gt(dto.stateField(), dto.stateFieldValue()));
        while (offset <= count) {
            iterDoc = collection.find(Filters.and(Filters.gte(dto.stateField(), dto.stateFieldValue())))
                    .skip(offset).limit(BATCH_SIZE);
            listLogs.addAll(iterDoc.map(Document::toJson)
                    .into(new ArrayList<>()));
            offset = offset + BATCH_SIZE;
        }
        client.close();
        return listLogs;
    }
}

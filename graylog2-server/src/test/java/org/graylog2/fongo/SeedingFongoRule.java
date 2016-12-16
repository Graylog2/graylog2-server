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
package org.graylog2.fongo;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fakemongo.junit.FongoRule;
import com.google.common.io.Resources;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.bson.json.JsonWriterSettings;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.MongoConnectionForTests;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Extends the regular {@link FongoRule} with seeding features.
 *
 * Every seed is a filename to a JSON file that contains a JSON object like the following.
 *
 * <pre>
 *     {
 *         "collectionName": [
 *             {"_id": {"$oid": "abcde..."}, "field1": "foo"},
 *             ...
 *         ]
 *     }
 * </pre>
 */
public class SeedingFongoRule extends FongoRule {
    private static final Logger LOG = LoggerFactory.getLogger(SeedingFongoRule.class);

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<Map<String, Object>>() {
    };

    private final List<String> seeds = new ArrayList<>();
    private final String dbName;
    private final ObjectMapper objectMapper;

    public static SeedingFongoRule create(String dbName) {
        return new SeedingFongoRule(dbName);
    }

    public SeedingFongoRule(String dbName) {
        super(dbName);
        this.dbName = dbName;
        this.objectMapper = new ObjectMapper();
    }

    public MongoConnection getConnection() {
        return new MongoConnectionForTests(getMongoClient(), dbName);
    }

    public SeedingFongoRule addSeed(String filename) {
        this.seeds.add(filename);
        return this;
    }

    @Override
    protected void before() throws UnknownHostException {
        super.before();

        for (final String seed : seeds) {
            try {
                insertSeed(seed);
            } catch (IOException e) {
                LOG.error("Unable to insert seed into database", e);
            }
        }
    }

    public void insertSeed(String seed) throws IOException {
        final byte[] bytes = Resources.toByteArray(Resources.getResource(seed));
        final Map<String, Object> map = objectMapper.readValue(bytes, MAP_TYPE);

        for (String collectionName : map.keySet()) {
            @SuppressWarnings("unchecked")
            final List<Map<String, Object>> documents = (List<Map<String, Object>>) map.get(collectionName);
            final MongoCollection<Document> indexSets = getDatabase().getCollection(collectionName);

            for (Map<String, Object> document : documents) {
                final Document parsedDocument = Document.parse(objectMapper.writeValueAsString(document));
                LOG.debug("Inserting parsed document: \n{}", parsedDocument.toJson(new JsonWriterSettings(true)));
                indexSets.insertOne(parsedDocument);
            }
        }
    }
}

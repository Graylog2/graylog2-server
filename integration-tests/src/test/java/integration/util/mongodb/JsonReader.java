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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.BasicDBObject;
import com.mongodb.util.JSON;
import org.bson.Document;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JsonReader implements DumpReader {
    private final Map<String, List<Document>> collectionMap = new HashMap<>();

    public JsonReader(URL location) throws IOException {
        final File file = new File(location.getPath());

        final ObjectMapper mapper = new ObjectMapper();
        final TypeReference ref = new TypeReference<Map<String, List<Map<String, Object>>>>(){};
        final Map<String, List<Map<String, Object>>> rawMap = mapper.readValue(file, ref);

        for (Map.Entry<String, List<Map<String, Object>>> entry : rawMap.entrySet()) {
            if (!collectionMap.containsKey(entry.getKey()))
                collectionMap.put(entry.getKey(), new ArrayList<Document>());

            for (Map<String, Object> rawDoc : entry.getValue()) {
                final BasicDBObject dbObject = (BasicDBObject)JSON.parse(mapper.writeValueAsString(rawDoc));
                collectionMap.get(entry.getKey()).add(new Document(dbObject.toMap()));
            }
        }
    }

    public Map<String, List<Document>> toMap() {
        return collectionMap;
    }
}

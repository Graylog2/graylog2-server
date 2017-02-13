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
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.apache.commons.io.FilenameUtils;
import org.bson.BSONDecoder;
import org.bson.BSONObject;
import org.bson.BasicBSONDecoder;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BsonReader implements DumpReader {
    private final Map<String, List<DBObject>> collectionMap;

    public BsonReader(URI location) {
        final File dir = new File(location);
        collectionMap = readBsonDirectory(dir);
    }

    protected List<DBObject> readBsonFile(String filename) {
        Path filePath = Paths.get(filename);
        List<DBObject> dataset = new ArrayList<>();

        try {
            ByteArrayInputStream fileBytes = new ByteArrayInputStream(Files.readAllBytes(filePath));
            BSONDecoder decoder = new BasicBSONDecoder();
            BSONObject obj;

            while ((obj = decoder.readObject(fileBytes)) != null) {
                final DBObject mongoDocument = new BasicDBObject(obj.toMap());
                dataset.add(mongoDocument);
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

    @Nullable
    protected Map<String, List<DBObject>> readBsonDirectory(File directory) {
        final Map<String, List<DBObject>> collections = new HashMap<>();

        final File[] collectionListing = directory.listFiles((dir, name) -> (name.endsWith(".bson") && !name.startsWith("system.indexes.")));

        if (collectionListing != null) {
            for (File collection : collectionListing) {
                List<DBObject> collectionData = readBsonFile(collection.getAbsolutePath());
                collections.put(FilenameUtils.removeExtension(collection.getName()), collectionData);
            }
        }

        return collections;
    }

    @Override
    public Map<String, List<DBObject>> toMap() {
        return collectionMap;
    }
}

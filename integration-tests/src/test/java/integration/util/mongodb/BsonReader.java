package integration.util.mongodb;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.commons.io.FilenameUtils;
import org.bson.BSONDecoder;
import org.bson.BSONObject;
import org.bson.BasicBSONDecoder;
import org.bson.Document;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BsonReader implements DumpReader {
    private final Map<String, List<Document>> collectionMap;

    public BsonReader(URL location) {
        final File dir = new File(location.getPath());
        collectionMap = readBsonDirectory(dir);
    }

    protected List<Document> readBsonFile(String filename){
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

    protected Map<String, List<Document>> readBsonDirectory(File directory) {
        final Map<String, List<Document>> collections = new HashMap<>();

        File[] collectionListing = directory.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return (name.endsWith(".bson") && !name.startsWith("system.indexes."));
            }
        });

        if (collectionListing != null) {
            for (File collection : collectionListing) {
                List<Document> collectionData = readBsonFile(collection.getAbsolutePath());
                collections.put(FilenameUtils.removeExtension(collection.getName()), collectionData);
            }
        }

        return collections;
    }

    public Map<String, List<Document>> toMap() {
        return collectionMap;
    }
}

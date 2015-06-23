package integration.util.mongodb;

import org.bson.Document;

import java.util.List;
import java.util.Map;

public interface DumpReader {
    Map<String, List<Document>> toMap();
}

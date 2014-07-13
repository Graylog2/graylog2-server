package org.graylog2.streams;

import org.bson.types.ObjectId;
import org.graylog2.database.CollectionName;
import org.graylog2.database.PersistedImpl;
import org.graylog2.database.validators.ClassNameStringValidator;
import org.graylog2.database.validators.FilledStringValidator;
import org.graylog2.database.validators.ObjectIdValidator;
import org.graylog2.plugin.database.validators.Validator;
import org.graylog2.plugin.outputs.MessageOutput;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.plugin.streams.StreamOutput;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
@CollectionName("streamoutputs")
public class StreamOutputImpl implements StreamOutput {
    private ObjectId _id;
    private String title;
    private String type;
    private Set<ObjectId> streams;
    private Map<String, Object> configuration;

    public StreamOutputImpl() {
    }

    public StreamOutputImpl(String title, String type, Set<ObjectId> streams, Map<String, Object> configuration) {
        this.title = title;
        this.type = type;
        this.streams = streams;
        this.configuration = configuration;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Set<String> getStreams() {
        Set<String> result = new HashSet<>();
        for (ObjectId oid : streams)
            result.add(oid.toStringMongod());
        return result;
    }

    public void setStreams(Set<ObjectId> streams) {
        this.streams = streams;
    }

    public Map<String, Object> getConfiguration() {
        return configuration;
    }

    @Override
    public void addStream(Stream stream) {
        streams.add(new ObjectId(stream.getId()));
    }

    public void setConfiguration(Map<String, Object> configuration) {
        this.configuration = configuration;
    }

    @Override
    public Map<String, Validator> getValidations() {
        return new HashMap<String, Validator>() {{
            put("title", new FilledStringValidator());
            put("type", new ClassNameStringValidator(MessageOutput.class));
            put("stream_id", new ObjectIdValidator());
        }};
    }

    @Override
    public Map<String, Validator> getEmbeddedValidations(String key) {
        return new HashMap<>();
    }

    @Override
    public String getId() {
        return _id.toStringMongod();
    }

    @Override
    public Map<String, Object> getFields() {
        return new HashMap<>();
    }

    @Override
    public Map<String, Object> asMap() {
        return new HashMap<>();
    }
}

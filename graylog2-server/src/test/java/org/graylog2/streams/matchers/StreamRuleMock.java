package org.graylog2.streams.matchers;

import com.mongodb.DBObject;
import org.bson.types.ObjectId;
import org.graylog2.plugin.streams.StreamRule;
import org.graylog2.plugin.streams.StreamRuleType;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class StreamRuleMock implements StreamRule {
    private ObjectId objectId = null;
    private ObjectId streamId = null;
    private StreamRuleType type = null;
    private String value = null;
    private String field = null;
    private Boolean inverted = false;

    public StreamRuleMock(DBObject rule) {
        this.objectId = (ObjectId) rule.get("_id");
        if (rule.get("type") != null)
            this.type = StreamRuleType.fromInteger((Integer) rule.get("type"));
        this.value = (String) rule.get("value");
        this.field = (String) rule.get("field");
        this.inverted = (Boolean) rule.get("inverted");
    }

    public ObjectId getObjectId() {
        return objectId;
    }

    public ObjectId getStreamId() {
        return streamId;
    }

    public StreamRuleType getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    public String getField() {
        return field;
    }

    public Boolean getInverted() {
        if (inverted == null)
            return false;
        return inverted;
    }

    public void setObjectId(ObjectId objectId) {
        this.objectId = objectId;
    }

    public void setStreamId(ObjectId streamId) {
        this.streamId = streamId;
    }

    public void setType(StreamRuleType type) {
        this.type = type;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setField(String field) {
        this.field = field;
    }

    public void setInverted(Boolean inverted) {
        this.inverted = inverted;
    }
}

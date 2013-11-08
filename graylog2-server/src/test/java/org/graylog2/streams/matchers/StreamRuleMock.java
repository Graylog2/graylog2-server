package org.graylog2.streams.matchers;

import com.mongodb.DBObject;
import org.bson.types.ObjectId;
import org.graylog2.plugin.streams.StreamRule;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class StreamRuleMock implements StreamRule {
    private ObjectId objectId = null;
    private Integer type = 0;
    private String value = null;
    private String field = null;
    private Boolean inverted = false;

    public StreamRuleMock(DBObject rule) {
        this.objectId = (ObjectId) rule.get("_id");
        this.type = (Integer) rule.get("type");
        this.value = (String) rule.get("value");
        this.field = (String) rule.get("field");
        this.inverted = (Boolean) rule.get("inverted");
    }

    public ObjectId getObjectId() {
        return objectId;
    }

    public Integer getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    public String getField() {
        return field;
    }

    public Boolean getInverted() {
        return inverted;
    }
}

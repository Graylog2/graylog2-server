package org.graylog2.streams.matchers;

import com.mongodb.DBObject;
import org.graylog2.plugin.streams.StreamRule;
import org.graylog2.plugin.streams.StreamRuleType;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class StreamRuleMock implements StreamRule {
    private String id;
    private String streamId;
    private StreamRuleType type = null;
    private String value;
    private String field;
    private Boolean inverted;

    public StreamRuleMock(DBObject rule) {
        this.id = rule.get("_id").toString();
        if (rule.get("type") != null)
            this.type = StreamRuleType.fromInteger((Integer) rule.get("type"));
        this.value = (String) rule.get("value");
        this.field = (String) rule.get("field");
        this.inverted = (Boolean) rule.get("inverted");
    }

    public String getId() {
        return id;
    }

    public String getStreamId() {
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

    public void setStreamId(String streamId) {
        this.streamId = streamId;
    }
}

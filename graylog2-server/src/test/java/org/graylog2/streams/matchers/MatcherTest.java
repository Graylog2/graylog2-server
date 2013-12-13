package org.graylog2.streams.matchers;

import com.mongodb.BasicDBObject;
import org.bson.types.ObjectId;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.streams.StreamRule;
import org.graylog2.streams.InvalidStreamRuleTypeException;
import org.graylog2.streams.StreamRuleMatcherFactory;
import org.joda.time.DateTime;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class MatcherTest {
    protected StreamRule getSampleRule() {
        BasicDBObject mongoRule = new BasicDBObject();
        mongoRule.put("_id", new ObjectId());
        mongoRule.put("field", "something");

        return new StreamRuleMock(mongoRule);
    }

    protected Message getSampleMessage() {
        return new Message("foo", "bar", new DateTime());
    }

    protected StreamRuleMatcher getMatcher(StreamRule rule) {
        StreamRuleMatcher matcher;
        try {
            matcher = StreamRuleMatcherFactory.build(rule.getType());
        } catch(InvalidStreamRuleTypeException e) {
            return null;
        }

        return matcher;
    }
}

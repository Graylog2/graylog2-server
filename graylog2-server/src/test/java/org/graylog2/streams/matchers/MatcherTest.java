package org.graylog2.streams.matchers;

import com.google.common.collect.Maps;
import org.bson.types.ObjectId;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.streams.StreamRule;
import org.graylog2.streams.InvalidStreamRuleTypeException;
import org.graylog2.streams.StreamRuleMatcherFactory;

import java.util.Map;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class MatcherTest {
    protected StreamRule getSampleRule() {
        Map<String, Object> mongoRule = Maps.newHashMap();
        mongoRule.put("_id", new ObjectId());
        mongoRule.put("field", "something");

        return new StreamRuleMock(mongoRule);
    }

    protected Message getSampleMessage() {
        return new Message("foo", "bar", Tools.iso8601());
    }

    protected StreamRuleMatcher getMatcher(StreamRule rule) {
        StreamRuleMatcher matcher;
        try {
            matcher = StreamRuleMatcherFactory.build(rule.getType());
        } catch (InvalidStreamRuleTypeException e) {
            return null;
        }

        return matcher;
    }
}

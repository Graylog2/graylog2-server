package org.graylog2.streams.matchers;

import org.graylog2.plugin.Message;
import org.graylog2.plugin.streams.StreamRule;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class FieldPresenceMatcher implements StreamRuleMatcher {
    @Override
    public boolean match(Message msg, StreamRule rule) {
        Object rawField = msg.getField(rule.getField());

        if (rawField == null) {
            return rule.getInverted();
        }

        if (rawField instanceof String) {
            String field = (String) rawField;
            Boolean result = rule.getInverted() ^ !(field.trim().isEmpty());
            return result;
        }

        return !rule.getInverted();
    }
}

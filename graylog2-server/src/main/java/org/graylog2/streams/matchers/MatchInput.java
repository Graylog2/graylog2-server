package org.graylog2.streams.matchers;

import org.graylog2.plugin.Message;
import org.graylog2.plugin.streams.StreamRule;

public class MatchInput implements StreamRuleMatcher {

    @Override
    public boolean match(Message msg, StreamRule rule) {
       if(msg.getField(Message.FIELD_GL2_SOURCE_INPUT) == null) {
           return rule.getInverted();
       }
        final String value = msg.getField(Message.FIELD_GL2_SOURCE_INPUT).toString();
        return rule.getInverted() ^ value.trim().equals(rule.getValue());
    }
}

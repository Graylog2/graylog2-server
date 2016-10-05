package org.graylog2.streams;

import org.bson.types.ObjectId;
import org.graylog2.plugin.streams.StreamRule;

public class StreamTestHelpers {
    public static StreamRule.Builder streamRuleBuilder() {
        return StreamRuleImpl.builder()
            .id(new ObjectId().toHexString())
            .streamId("deadbeefdeadbeef");
    }
}

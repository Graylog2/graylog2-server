/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
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
        return new Message("foo", "bar", Tools.nowUTC());
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

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

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.streams.StreamRule;
import org.graylog2.utilities.InterruptibleCharSequence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class RegexMatcher implements StreamRuleMatcher {
    private static final Logger LOG = LoggerFactory.getLogger(RegexMatcher.class);

    private static final long CACHESIZE = 1000;
    private static final LoadingCache<String, Pattern> patternCache = CacheBuilder.newBuilder().maximumSize(CACHESIZE).build(new CacheLoader<String, Pattern>() {
        @Override
        public Pattern load(String key) throws Exception {
            return Pattern.compile(key, Pattern.DOTALL);
        }
    });

    @Override
    public boolean match(Message msg, StreamRule rule) {
        if (msg.getField(rule.getField()) == null)
            return rule.getInverted();

        try {
            final Pattern pattern = patternCache.get(rule.getValue());
            final CharSequence charSequence = new InterruptibleCharSequence(msg.getField(rule.getField()).toString());
            return rule.getInverted() ^ pattern.matcher(charSequence).find();
        } catch (ExecutionException e) {
            LOG.error("Unable to get pattern from regex cache: ", e);
        }

        return false;
    }

}

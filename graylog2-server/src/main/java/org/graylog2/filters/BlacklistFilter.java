/*
 * Copyright 2012-2014 TORCH GmbH
 *
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.graylog2.filters;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.graylog2.blacklists.Blacklist;
import org.graylog2.blacklists.BlacklistRule;
import org.graylog2.plugin.GraylogServer;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.filters.MessageFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class BlacklistFilter implements MessageFilter {
    
    private static final Logger LOG = LoggerFactory.getLogger(BlacklistFilter.class);
    private static final long CACHESIZE = 1000;

    private final LoadingCache<String, Pattern> patternCache;

    public BlacklistFilter() {
        this.patternCache = CacheBuilder.newBuilder().maximumSize(CACHESIZE).build(new CacheLoader<String, Pattern>() {
            @Override
            public Pattern load(String key) throws Exception {
                return Pattern.compile(key, Pattern.DOTALL);
            }
        });
    }

    @Override
    public boolean filter(Message msg, GraylogServer server) {
        for (Blacklist blacklist : Blacklist.fetchAll()) {
            for (BlacklistRule rule : blacklist.getRules()) {
                Pattern pattern;
                try {
                    pattern = this.patternCache.get(rule.getTerm());
                } catch (ExecutionException e) {
                    LOG.error("Unable to get regex from cache: ", e);
                    continue;
                }

                if (pattern.matcher(msg.getMessage()).matches()) {
                    LOG.debug("Message <{}> is blacklisted. First match on {}", this, rule.getTerm());

                    // Done - This message is blacklisted.
                    return true;
                }
            }
        }

        return false;
    }
    
    @Override
    public String getName() {
        return "Blacklister";
    }

}

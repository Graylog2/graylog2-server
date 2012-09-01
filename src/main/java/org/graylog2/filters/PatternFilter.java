/**
 * Copyright 2012 Nikolay Bryskin <devel.niks@gmail.com>
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
 *
 */

package org.graylog2.filters;

import java.util.List;
import com.google.common.collect.Lists;
import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Timer;
import com.yammer.metrics.core.TimerContext;
import java.util.concurrent.TimeUnit;
import org.apache.log4j.Logger;
import org.graylog2.GraylogServer;
import org.graylog2.logmessage.LogMessage;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.graylog2.initializers.Initializer;
import org.graylog2.Configuration;
import java.io.BufferedReader;
import java.io.FileReader;

/**
 * @author Nikolay Bryskin <devel.niks@gmail.com>
 */
public class PatternFilter implements MessageFilter, Initializer {

    private static final Logger LOG = Logger.getLogger(PatternFilter.class);
    protected static final Timer processTimer = Metrics.newTimer(PatternFilter.class, "ProcessTime", TimeUnit.MICROSECONDS, TimeUnit.SECONDS);
    private class MessagePattern {
        public Pattern regex;
        public List<String> groups;

        public MessagePattern() {
            groups = Lists.newArrayList();
        }
    }
    protected List<MessagePattern> patterns;
    protected final Configuration configuration;
    protected static final Pattern groupPattern = Pattern.compile("\\(\\?<(\\w+)>");

    public PatternFilter(Configuration configuration) {
        this.configuration = configuration;
        this.patterns = Lists.newArrayList();
    }

    @Override
    public boolean masterOnly() {
        return false;
    }

    @Override
    public void initialize() {
	try {
            BufferedReader reader = new BufferedReader(new FileReader(configuration.getPatternRulesFile()));
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
               LOG.info("parsing "+line);
               MessagePattern pattern = new MessagePattern();
               pattern.regex = Pattern.compile(line);
               Matcher groupMatcher = groupPattern.matcher(line);
               while (groupMatcher.find()) {
                    String group = groupMatcher.group(1);
                    pattern.groups.add(group);
                    LOG.info("added group "+group);
                    this.patterns.add(pattern);    
                }
            }
            LOG.info("Initialized pattern engine");
        } catch (Exception e) {
            LOG.fatal("Could not load pattern engine: " + e.getMessage(), e);
            System.exit(1);
        }
    }

    @Override
    public boolean filter(LogMessage msg, GraylogServer server) {
        TimerContext tcx = processTimer.time();

	for (MessagePattern pattern : this.patterns) {
		parseMessage(msg, pattern);
	}

        tcx.stop();

        // Do not discard message.
        return false;
    }

    private void parseMessage(LogMessage m, MessagePattern pattern) {
	Matcher matcher = pattern.regex.matcher(m.getShortMessage());
	if (matcher.find()) {
            //LOG.info("Matched "+m.getShortMessage()+" against "+pattern.regex.pattern());
		for (String group : pattern.groups) {
                        String data = matcher.group(group);
                        if (data != null) {
                            m.addAdditionalData(group.replace("X", "_"), data);
                        }
		}
	} else {
            //LOG.info("Failed to match "+m.getShortMessage()+" against "+pattern.regex.pattern());
        }
}
}

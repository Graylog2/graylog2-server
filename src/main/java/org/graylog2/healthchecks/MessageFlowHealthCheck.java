/**
 * Copyright 2012 Lennart Koopmann <lennart@socketfeed.com>
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

package org.graylog2.healthchecks;

import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.HealthCheck;
import com.yammer.metrics.core.Meter;
import com.yammer.metrics.core.MetricName;
import org.graylog2.buffers.processors.ProcessBufferProcessor;

/**
 * MessageFlowHealthCheck.java: 19.06.2012 16:04:38.
 *
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class MessageFlowHealthCheck extends HealthCheck {

    public MessageFlowHealthCheck() {
        super("messageFlow");
    }

    @Override
    protected Result check() throws Exception {
        MetricName name = new MetricName(ProcessBufferProcessor.class, "IncomingMessagesMinutely");
        Meter m = (Meter) Metrics.defaultRegistry().allMetrics().get(name);
        
        if (m == null) {
            return Result.unhealthy("No messages at all yet");
        }

        if (String.valueOf(m.oneMinuteRate()).startsWith("0")) {
            return Result.unhealthy("Message flow zero (" + m.oneMinuteRate() + ")");
        }

        return Result.healthy(String.valueOf(m.oneMinuteRate()));
    }

}

/**
 * The MIT License
 * Copyright (c) 2012 Graylog, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.graylog2.plugin.alarms;

import org.graylog2.plugin.MessageSummary;
import org.graylog2.plugin.streams.Stream;
import org.joda.time.DateTime;

import java.util.List;
import java.util.Map;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public interface AlertCondition {
    String getDescription();

    String getId();

    DateTime getCreatedAt();

    String getCreatorUserId();

    Stream getStream();

    Map<String, Object> getParameters();

    Integer getBacklog();

    int getGrace();

    String getTypeString();

    interface CheckResult {
        boolean isTriggered();
        String getResultDescription();
        AlertCondition getTriggeredCondition();
        DateTime getTriggeredAt();

        /**
         * The limited list of messages that matched the alert in the corresponding stream.
         *
         * @return list of message summaries
         */
        List<MessageSummary> getMatchingMessages();
    }
}

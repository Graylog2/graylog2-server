/**
 * The MIT License
 * Copyright (c) 2012 TORCH GmbH
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
package org.graylog2.restclient.models;

import org.graylog2.restclient.models.api.responses.system.SystemMessageSummaryResponse;
import org.joda.time.DateTime;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class SystemMessage {

    // This must match the messages returned per page by server or things will go horribly wrong.
    public static final int PER_PAGE = 30;

    private DateTime timestamp;
    private String caller;
    private String content;
    private String nodeId;

    public SystemMessage(SystemMessageSummaryResponse sms) {
        this.timestamp = new DateTime(sms.timestamp);
        this.caller = sms.caller;
        this.content = sms.content;
        this.nodeId = sms.nodeId;
    }

    public SystemMessage(SystemMessage sm) {
        this.timestamp = sm.timestamp;
        this.caller = sm.caller;
        this.content = sm.content;
        this.nodeId = sm.nodeId;
    }

    public DateTime getTimestamp() {
        return timestamp;
    }

    public String getCaller() {
        return caller;
    }

    public String getContent() {
        return content;
    }

    public String getNodeId() {
        return nodeId;
    }
}

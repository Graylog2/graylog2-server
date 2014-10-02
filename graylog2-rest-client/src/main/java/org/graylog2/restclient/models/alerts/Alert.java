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
package org.graylog2.restclient.models.alerts;

import org.graylog2.restclient.models.api.responses.alerts.AlertSummaryResponse;
import org.joda.time.DateTime;

import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class Alert {

    private final String id;
    private final String streamId;
    private final String conditionId;
    private final DateTime triggeredAt;
    private final String description;
    private final Map<String,Object> conditionParameters;

    public Alert(AlertSummaryResponse asr) {
        this.id = asr.id;
        this.streamId = asr.streamId;
        this.conditionId = asr.conditionId;
        this.triggeredAt = DateTime.parse(asr.triggeredAt);
        this.description = asr.description;
        this.conditionParameters = asr.conditionParameters;
    }

    public Map<String, Object> getConditionParameters() {
        return conditionParameters;
    }

    public String getDescription() {
        return description;
    }

    public DateTime getTriggeredAt() {
        return triggeredAt;
    }

    public String getConditionId() {
        return conditionId;
    }

    public String getStreamId() {
        return streamId;
    }

    public String getId() {
        return id;
    }

}

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
package org.graylog2.plugin.inputs;

import com.google.common.collect.Maps;
import org.graylog2.plugin.Tools;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.Map;
import java.util.UUID;

public class InputState {
    public enum InputStateType {
        CREATED,
        INITIALIZED,
        INVALID_CONFIGURATION,
        STARTING,
        RUNNING,
        FAILED,
        STOPPED,
        TERMINATED
    }

    protected MessageInput messageInput;
    protected final String id;
    protected InputStateType state;
    protected DateTime startedAt;
    protected String detailedMessage;

    public InputState(MessageInput input) {
        this(input, InputStateType.CREATED);
    }

    public InputState(MessageInput input, InputStateType state) {
        this(input, state, UUID.randomUUID().toString());
    }

    public InputState(MessageInput input, String id) {
        this(input, InputStateType.CREATED, id);
    }

    public InputState(MessageInput input, InputStateType state, String id) {
        this.state = state;
        this.messageInput = input;
        this.id = id;
        this.startedAt = Tools.iso8601();
    }


    public MessageInput getMessageInput() {
        return messageInput;
    }

    public void setMessageInput(MessageInput messageInput) {
        this.messageInput = messageInput;
    }

    public String getId() {
        return id;
    }

    public InputStateType getState() {
        return state;
    }

    public void setState(InputStateType state) {
        this.state = state;
        this.setDetailedMessage(null);
    }

    public DateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(DateTime startedAt) {
        this.startedAt = startedAt;
    }

    public String getDetailedMessage() {
        return detailedMessage;
    }

    public void setDetailedMessage(String detailedMessage) {
        this.detailedMessage = detailedMessage;
    }

    public Map<String, Object> asMap() {
        Map<String, Object> inputStateMap = Maps.newHashMap();
        inputStateMap.put("id", id);
        inputStateMap.put("state", state.toString().toLowerCase());
        inputStateMap.put("started_at", Tools.getISO8601String(startedAt));
        inputStateMap.put("message_input", messageInput.asMap());
        inputStateMap.put("detailed_message", detailedMessage);

        return inputStateMap;
    }

    @Override
    public String toString() {
        return "InputState{" +
                "messageInput=" + messageInput +
                ", id='" + id + '\'' +
                ", state=" + state +
                ", startedAt=" + startedAt +
                ", detailedMessage='" + detailedMessage + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        InputState that = (InputState) o;

        if (!id.equals(that.id)) return false;
        if (!messageInput.equals(that.messageInput)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = messageInput.hashCode();
        result = 31 * result + id.hashCode();
        return result;
    }
}

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

package org.graylog2.plugin.inputs;

import com.google.common.collect.Maps;
import org.graylog2.plugin.Tools;
import org.joda.time.DateTime;

import java.util.Map;
import java.util.UUID;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
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
        this.startedAt = DateTime.now();
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

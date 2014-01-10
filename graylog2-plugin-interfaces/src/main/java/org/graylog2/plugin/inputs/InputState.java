package org.graylog2.plugin.inputs;

import com.google.common.collect.Maps;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.inputs.MessageInput;
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
        STARTING,
        RUNNING,
        FAILED,
        STOPPED
    }

    protected MessageInput messageInput;
    protected String id;
    protected InputStateType state;
    protected String startedAt;
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
        this.startedAt = Tools.getISO8601String(DateTime.now());
    }


    public MessageInput getMessageInput() {
        return messageInput;
    }

    public String getId() {
        return id;
    }

    public InputStateType getState() {
        return state;
    }

    public void setState(InputStateType state) {
        this.state = state;
    }

    public String getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(String startedAt) {
        this.startedAt = startedAt;
    }

    public Map<String, Object> asMap() {
        Map<String, Object> inputStateMap = Maps.newHashMap();
        inputStateMap.put("id", id);
        inputStateMap.put("state", state.toString().toLowerCase());
        inputStateMap.put("started_at", startedAt);
        inputStateMap.put("message_input", messageInput.asMap());

        return inputStateMap;
    }
}

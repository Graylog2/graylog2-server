package org.graylog2.restclient.models;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog2.restclient.lib.ApiClient;
import org.graylog2.restclient.models.api.responses.system.InputStateSummaryResponse;
import org.joda.time.DateTime;
import org.slf4j.LoggerFactory;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class InputState {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(Input.class);

    public interface Factory {
        InputState fromSummaryResponse(InputStateSummaryResponse input, ClusterEntity node);
    }

    public enum InputStateType {
        CREATED,
        INITIALIZED,
        STARTING,
        RUNNING,
        FAILED,
        STOPPED,
        TERMINATED;

        @Override
        public String toString() {
            return super.toString().toLowerCase();
        }
    }

    private final ApiClient api;
    private final UniversalSearch.Factory searchFactory;
    private final Input.Factory inputFactory;
    private final ClusterEntity node;
    private final UserService userService;

    private final String id;
    private final DateTime startedAt;
    private final InputStateType state;
    private final Input input;
    private final String detailedMessage;

    @AssistedInject
    private InputState(ApiClient api,
                       UniversalSearch.Factory searchFactory,
                       Input.Factory inputFactory,
                       UserService userService,
                       @Assisted InputStateSummaryResponse issr,
                       @Assisted ClusterEntity node) {
        this.api = api;
        this.searchFactory = searchFactory;
        this.inputFactory = inputFactory;
        this.userService = userService;
        this.node = node;

        this.id = issr.id;
        this.state = InputStateType.valueOf(issr.state.toUpperCase());
        this.startedAt = DateTime.parse(issr.startedAt);
        this.input = inputFactory.fromSummaryResponse(issr.messageinput, node);
        this.detailedMessage = issr.detailedMessage;
    }

    public String getId() {
        return id;
    }

    public DateTime getStartedAt() {
        return startedAt;
    }

    public InputStateType getState() {
        return state;
    }

    public Input getInput() {
        return input;
    }

    public ClusterEntity getNode() {
        return node;
    }

    public String getDetailedMessage() {
        return detailedMessage;
    }
}

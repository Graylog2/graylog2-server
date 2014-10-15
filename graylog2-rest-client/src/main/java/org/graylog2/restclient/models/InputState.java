/**
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

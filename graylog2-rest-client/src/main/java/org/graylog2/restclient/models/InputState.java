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

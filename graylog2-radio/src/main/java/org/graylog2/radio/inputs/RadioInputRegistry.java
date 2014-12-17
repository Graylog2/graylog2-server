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
package org.graylog2.radio.inputs;

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.Lists;
import com.ning.http.client.AsyncHttpClient;
import org.graylog2.plugin.buffers.InputBuffer;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationException;
import org.graylog2.plugin.IOState;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.radio.cluster.InputService;
import org.graylog2.radio.inputs.api.InputSummaryResponse;
import org.graylog2.radio.inputs.api.RegisterInputResponse;
import org.graylog2.shared.inputs.InputRegistry;
import org.graylog2.shared.inputs.MessageInputFactory;
import org.graylog2.shared.inputs.NoSuchInputTypeException;
import org.graylog2.shared.inputs.PersistedInputs;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.net.URI;
import java.util.List;

public class RadioInputRegistry extends InputRegistry {
    private static final Logger LOG = LoggerFactory.getLogger(RadioInputRegistry.class);

    protected final AsyncHttpClient httpclient;
    protected final URI serverUrl;
    private final InputService inputService;

    @Inject
    public RadioInputRegistry(IOState.Factory<MessageInput> inputStateFactory,
                              InputBuffer inputBuffer,
                              AsyncHttpClient httpclient,
                              URI serverUrl,
                              InputService inputService,
                              MetricRegistry metricRegistry,
                              PersistedInputs persistedInputs) {
        super(inputStateFactory, inputBuffer, metricRegistry, persistedInputs);
        this.httpclient = httpclient;
        this.serverUrl = serverUrl;
        this.inputService = inputService;
    }

    @Override
    public IOState<MessageInput> launch(MessageInput input, String id, boolean register) {
        if (register) {
            try {
                final RegisterInputResponse response = inputService.registerInCluster(input);
                if (response != null)
                    input.setPersistId(response.persistId);
            } catch (Exception e) {
                LOG.error("Could not register input in Graylog2 cluster. It will be lost on next restart of this radio node.", e);
                return null;
            }
        }
        return super.launch(input, id, register);
    }
}

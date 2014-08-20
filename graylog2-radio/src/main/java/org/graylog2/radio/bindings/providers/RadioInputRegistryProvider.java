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
package org.graylog2.radio.bindings.providers;

import com.ning.http.client.AsyncHttpClient;
import org.graylog2.radio.Configuration;
import org.graylog2.radio.inputs.RadioInputRegistry;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.shared.buffers.ProcessBuffer;
import org.graylog2.shared.inputs.InputRegistry;
import org.graylog2.shared.inputs.MessageInputFactory;

import javax.inject.Inject;
import javax.inject.Provider;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class RadioInputRegistryProvider implements Provider<InputRegistry> {
    private static RadioInputRegistry radioInputRegistry = null;

    @Inject
    public RadioInputRegistryProvider(MessageInputFactory messageInputFactory,
                                      ProcessBuffer processBuffer,
                                      AsyncHttpClient httpClient,
                                      Configuration configuration,
                                      ServerStatus serverStatus) {
        if (radioInputRegistry == null)
            radioInputRegistry = new RadioInputRegistry(messageInputFactory,
                    processBuffer,
                    httpClient,
                    configuration.getGraylog2ServerUri(),
                    serverStatus);
    }

    @Override
    public InputRegistry get() {
        return radioInputRegistry;
    }
}

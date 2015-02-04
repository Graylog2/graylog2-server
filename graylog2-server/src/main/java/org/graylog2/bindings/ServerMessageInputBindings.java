/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.bindings;

import com.google.inject.multibindings.MapBinder;
import org.graylog2.inputs.radio.RadioAMQPInput;
import org.graylog2.inputs.radio.RadioKafkaInput;
import org.graylog2.plugin.inject.Graylog2Module;
import org.graylog2.plugin.inputs.MessageInput;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class ServerMessageInputBindings extends Graylog2Module {
    @Override
    protected void configure() {
        final MapBinder<String, MessageInput.Factory<? extends MessageInput>> inputMapBinder = inputsMapBinder();
        installInput(inputMapBinder, RadioKafkaInput.class, RadioKafkaInput.Factory.class);
        installInput(inputMapBinder, RadioAMQPInput.class, RadioAMQPInput.Factory.class);
    }
}

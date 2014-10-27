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
package org.graylog2.shared.bindings;

import com.google.inject.multibindings.MapBinder;
import org.graylog2.inputs.amqp.AMQPInput;
import org.graylog2.inputs.codecs.CodecsModule;
import org.graylog2.inputs.gelf.http.GELFHttpInput;
import org.graylog2.inputs.gelf.tcp.GELFTCPInput;
import org.graylog2.inputs.gelf.udp.GELFUDPInput;
import org.graylog2.inputs.kafka.KafkaInput;
import org.graylog2.inputs.misc.jsonpath.JsonPathInput;
import org.graylog2.inputs.misc.metrics.LocalMetricsInput;
import org.graylog2.inputs.radio.RadioAMQPInput;
import org.graylog2.inputs.radio.RadioKafkaInput;
import org.graylog2.inputs.random.FakeHttpMessageInput;
import org.graylog2.inputs.raw.tcp.RawTCPInput;
import org.graylog2.inputs.raw.udp.RawUDPInput;
import org.graylog2.inputs.syslog.tcp.SyslogTCPInput;
import org.graylog2.inputs.syslog.udp.SyslogUDPInput;
import org.graylog2.inputs.transports.TransportsModule;
import org.graylog2.plugin.inject.Graylog2Module;
import org.graylog2.plugin.inputs.MessageInput;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class MessageInputBindings extends Graylog2Module {
    @Override
    protected void configure() {
        install(new TransportsModule());
        install(new CodecsModule());

        final MapBinder<String, MessageInput.Factory<? extends MessageInput>> inputMapBinder = inputsMapBinder();
        // new style inputs, using transports and codecs
        installInput(inputMapBinder, RawTCPInput.class, RawTCPInput.Factory.class);
        installInput(inputMapBinder, RawUDPInput.class, RawUDPInput.Factory.class);
        installInput(inputMapBinder, SyslogTCPInput.class, SyslogTCPInput.Factory.class);
        installInput(inputMapBinder, SyslogUDPInput.class, SyslogUDPInput.Factory.class);
        installInput(inputMapBinder, FakeHttpMessageInput.class, FakeHttpMessageInput.Factory.class);
        installInput(inputMapBinder, GELFTCPInput.class, GELFTCPInput.Factory.class);
        installInput(inputMapBinder, GELFHttpInput.class, GELFHttpInput.Factory.class);
        installInput(inputMapBinder, GELFUDPInput.class, GELFUDPInput.Factory.class);
        installInput(inputMapBinder, KafkaInput.class, KafkaInput.Factory.class);
        installInput(inputMapBinder, RadioKafkaInput.class, RadioKafkaInput.Factory.class);
        installInput(inputMapBinder, AMQPInput.class, AMQPInput.Factory.class);
        installInput(inputMapBinder, RadioAMQPInput.class, RadioAMQPInput.Factory.class);
        installInput(inputMapBinder, JsonPathInput.class, JsonPathInput.Factory.class);
        installInput(inputMapBinder, LocalMetricsInput.class, LocalMetricsInput.Factory.class);
    }


}

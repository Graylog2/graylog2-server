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

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.multibindings.Multibinder;
import org.graylog2.inputs.amqp.AMQPInput;
import org.graylog2.inputs.codecs.CodecsModule;
import org.graylog2.inputs.gelf.http.GELFHttpInput;
import org.graylog2.inputs.gelf.tcp.GELFTCPInput;
import org.graylog2.inputs.gelf.tcp.GELFTCPInput2;
import org.graylog2.inputs.gelf.udp.GELFUDPInput;
import org.graylog2.inputs.gelf.udp.GELFUDPInput2;
import org.graylog2.inputs.kafka.KafkaInput;
import org.graylog2.inputs.misc.jsonpath.JsonPathInput;
import org.graylog2.inputs.misc.metrics.LocalMetricsInput;
import org.graylog2.inputs.random.FakeHttpMessageInput;
import org.graylog2.inputs.random.FakeHttpMessageInput2;
import org.graylog2.inputs.raw.tcp.RawTCPInput;
import org.graylog2.inputs.raw.tcp.RawTCPInput2;
import org.graylog2.inputs.raw.udp.RawUDPInput;
import org.graylog2.inputs.raw.udp.RawUDPInput2;
import org.graylog2.inputs.syslog.tcp.SyslogTCPInput;
import org.graylog2.inputs.syslog.tcp.SyslogTCPInput2;
import org.graylog2.inputs.syslog.udp.SyslogUDPInput;
import org.graylog2.inputs.syslog.udp.SyslogUDPInput2;
import org.graylog2.inputs.transports.TransportsModule;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.MessageInput2;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class MessageInputBindings extends AbstractModule {
    @Override
    protected void configure() {
        TypeLiteral<Class<? extends MessageInput>> typeLiteral = new TypeLiteral<Class<? extends MessageInput>>() {
        };
        Multibinder<Class<? extends MessageInput>> messageInputs = Multibinder.newSetBinder(binder(), typeLiteral);
        messageInputs.addBinding().toInstance(GELFTCPInput.class);
        messageInputs.addBinding().toInstance(GELFUDPInput.class);
        messageInputs.addBinding().toInstance(GELFHttpInput.class);
        messageInputs.addBinding().toInstance(RawTCPInput.class);
        messageInputs.addBinding().toInstance(RawUDPInput.class);
        messageInputs.addBinding().toInstance(AMQPInput.class);
        messageInputs.addBinding().toInstance(KafkaInput.class);
        messageInputs.addBinding().toInstance(JsonPathInput.class);
        messageInputs.addBinding().toInstance(LocalMetricsInput.class);
        messageInputs.addBinding().toInstance(FakeHttpMessageInput.class);
        messageInputs.addBinding().toInstance(SyslogTCPInput.class);
        messageInputs.addBinding().toInstance(SyslogUDPInput.class);

        install(new TransportsModule());
        install(new CodecsModule());

        final MapBinder<String, MessageInput2.Factory<? extends MessageInput2>> inputMapBinder =
                MapBinder.newMapBinder(binder(),
                                       TypeLiteral.get(String.class),
                                       new TypeLiteral<MessageInput2.Factory<? extends MessageInput2>>() {
                                       });
        // new style inputs, using transports and codecs
        installInput(inputMapBinder, RawTCPInput2.class, RawTCPInput2.Factory.class);
        installInput(inputMapBinder, RawUDPInput2.class, RawUDPInput2.Factory.class);
        installInput(inputMapBinder, SyslogTCPInput2.class, SyslogTCPInput2.Factory.class);
        installInput(inputMapBinder, SyslogUDPInput2.class, SyslogUDPInput2.Factory.class);
        installInput(inputMapBinder, FakeHttpMessageInput2.class, FakeHttpMessageInput2.Factory.class);
        installInput(inputMapBinder, GELFTCPInput2.class, GELFTCPInput2.Factory.class);
        installInput(inputMapBinder, GELFUDPInput2.class, GELFUDPInput2.Factory.class);


    }

    private <T extends MessageInput2> void installInput(MapBinder<String, MessageInput2.Factory<? extends MessageInput2>> inputMapBinder,
                                                        Class<T> target,
                                                        Class<? extends MessageInput2.Factory<T>> targetFactory) {
        install(new FactoryModuleBuilder().implement(MessageInput2.class, target).build(targetFactory));
        inputMapBinder.addBinding(target.getCanonicalName()).to(Key.get(targetFactory));
    }


}

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
package org.graylog2.shared.bindings;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import org.graylog2.inputs.amqp.AMQPInput;
import org.graylog2.inputs.gelf.http.GELFHttpInput;
import org.graylog2.inputs.gelf.tcp.GELFTCPInput;
import org.graylog2.inputs.gelf.udp.GELFUDPInput;
import org.graylog2.inputs.kafka.KafkaInput;
import org.graylog2.inputs.misc.jsonpath.JsonPathInput;
import org.graylog2.inputs.misc.metrics.LocalMetricsInput;
import org.graylog2.inputs.random.FakeHttpMessageInput;
import org.graylog2.inputs.raw.tcp.RawTCPInput;
import org.graylog2.inputs.raw.udp.RawUDPInput;
import org.graylog2.inputs.syslog.tcp.SyslogTCPInput;
import org.graylog2.inputs.syslog.udp.SyslogUDPInput;
import org.graylog2.plugin.inputs.MessageInput;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class MessageInputBindings extends AbstractModule {
    @Override
    protected void configure() {
        TypeLiteral<Class<? extends MessageInput>> typeLiteral = new TypeLiteral<Class<? extends MessageInput>>(){};
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
    }
}

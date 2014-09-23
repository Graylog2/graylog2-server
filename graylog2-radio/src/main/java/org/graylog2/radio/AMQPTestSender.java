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
package org.graylog2.radio;

import com.codahale.metrics.Meter;
import com.google.inject.name.Named;
import org.graylog2.plugin.Message;
import org.graylog2.radio.transports.RadioTransport;
import org.joda.time.DateTime;

import javax.inject.Inject;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class AMQPTestSender implements Runnable {
    private final RadioTransport radioTransport;
    private final Message message;
    private final Meter meter;

    @Inject
    public AMQPTestSender(RadioTransport radioTransport, @Named("throughputMeter") Meter meter) {
        this.radioTransport = radioTransport;
        this.meter = meter;
        this.message = new Message("foobar", "localhost", DateTime.now());
    }

    @Override
    public void run() {
        int i = 0;
        while(true) {
            radioTransport.send(message);
            meter.mark();
        }
    }
}

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

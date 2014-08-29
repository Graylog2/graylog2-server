package org.graylog2.radio;

import com.codahale.metrics.Meter;
import com.github.joschi.jadconfig.JadConfig;
import com.google.common.util.concurrent.Uninterruptibles;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import org.graylog2.radio.bindings.RadioBindings;
import org.graylog2.radio.bindings.RadioInitializerBindings;
import org.graylog2.radio.transports.amqp.AMQPSender;
import org.graylog2.shared.bindings.GuiceInstantiationService;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class AMQPTestSenderRunner extends Main {
    public static class AMQPSenderProvider implements Provider<AMQPSender> {
        private final Configuration configuration;
        private final static AtomicInteger instanceCount;

        static {
            instanceCount = new AtomicInteger(0);
        }

        @Inject
        public AMQPSenderProvider(Configuration configuration) {
            this.configuration = configuration;
        }

        @Override
        public AMQPSender get() {
            final int instanceId = instanceCount.incrementAndGet();
            final String queueName = String.format(configuration.getAmqpQueueName(), instanceId);
            final String exchangeName = String.format(configuration.getAmqpExchangeName(), instanceId);
            System.out.println("Returning instance for id " + instanceCount + ", queueName: " + queueName + ", exchange: " + exchangeName);
            return new AMQPSender(
                    configuration.getAmqpHostname(),
                    configuration.getAmqpPort(),
                    configuration.getAmqpVirtualHost(),
                    configuration.getAmqpUsername(),
                    configuration.getAmqpPassword(),
                    queueName,
                    configuration.getAmqpQueueType(),
                    exchangeName,
                    configuration.getAmqpRoutingKey()
            );
        }
    }
    public static class Bindings extends AbstractModule {
        @Override
        protected void configure() {
            bind(Meter.class).annotatedWith(Names.named("throughputMeter")).toInstance(new Meter());
            bind(AMQPSender.class).toProvider(AMQPSenderProvider.class);
        }
    }

    public static class ThroughputDisplayer implements Runnable {
        private final Meter meter;

        @Inject
        public ThroughputDisplayer(@Named("throughputMeter") Meter meter) {
            this.meter = meter;
        }

        @Override
        public void run() {
            while(true) {
                System.out.println("Throughput: " + meter.getOneMinuteRate());
                Uninterruptibles.sleepUninterruptibly(1, TimeUnit.SECONDS);
            }
        }
    }
    public static void main(String[] argv) {
        final JadConfig jadConfig = new JadConfig();
        final Configuration configuration = readConfiguration(jadConfig, "/etc/graylog2-radio.conf");

        GuiceInstantiationService instantiationService = new GuiceInstantiationService();
        List<Module> bindingsModules = getBindingsModules(instantiationService,
                new RadioBindings(configuration),
                new RadioInitializerBindings(),
                new Bindings());

        Injector injector = Guice.createInjector(bindingsModules);

        int threadCount = Integer.parseInt(argv[1]);

        Thread[] threads = new Thread[threadCount];
        for (int i = 0; i < threadCount-1; i++) {
            threads[i] = new Thread(injector.getInstance(AMQPTestSender.class));
            threads[i].start();
        }

        Thread displayer = new Thread(injector.getInstance(ThroughputDisplayer.class));

        displayer.start();
    }
}

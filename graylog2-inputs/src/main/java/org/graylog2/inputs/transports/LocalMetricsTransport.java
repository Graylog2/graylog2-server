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
package org.graylog2.inputs.transports;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.MetricSet;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog2.inputs.misc.metrics.agent.GELFTarget;
import org.graylog2.inputs.misc.metrics.agent.Graylog2Reporter;
import org.graylog2.plugin.ConfigClass;
import org.graylog2.plugin.FactoryClass;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.configuration.fields.DropdownField;
import org.graylog2.plugin.configuration.fields.NumberField;
import org.graylog2.plugin.configuration.fields.TextField;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.MisfireException;
import org.graylog2.plugin.inputs.codecs.CodecAggregator;
import org.graylog2.plugin.inputs.transports.ThrottleableTransport;
import org.graylog2.plugin.inputs.transports.Transport;
import org.graylog2.plugin.journal.RawMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class LocalMetricsTransport extends ThrottleableTransport {
    private static final Logger log = LoggerFactory.getLogger(LocalMetricsTransport.class);

    private static final String CK_REPORT_INTERVAL = "report_interval";
    private static final String CK_REPORT_UNIT = "report_unit";
    private static final String CK_DURATION_UNIT = "duration_unit";
    private static final String CK_RATE_UNIT = "rate_unit";
    private static final String CK_SOURCE = "source";

    private final Configuration configuration;
    private final ObjectMapper mapper;
    private final MetricRegistry metricRegistry;
    private final ScheduledExecutorService scheduler;
    private Graylog2Reporter reporter;
    private ScheduledFuture<?> scheduledFuture;

    @AssistedInject
    public LocalMetricsTransport(@Assisted Configuration configuration,
                                 ObjectMapper mapper,
                                 MetricRegistry metricRegistry,
                                 @Named("daemonScheduler") ScheduledExecutorService scheduler) {
        this.configuration = configuration;
        this.mapper = mapper;
        this.metricRegistry = metricRegistry;
        this.scheduler = scheduler;
    }

    @Override
    public void setMessageAggregator(CodecAggregator aggregator) {
        // unsupported
    }

    @Override
    public void launch(final MessageInput input) throws MisfireException {
        reporter = Graylog2Reporter.forRegistry(metricRegistry)
                .useSource(configuration.getString(CK_SOURCE))
                .convertDurationsTo(TimeUnit.valueOf(configuration.getString(CK_DURATION_UNIT)))
                .convertRatesTo(TimeUnit.valueOf(configuration.getString(CK_RATE_UNIT)))
                .build(new RawGelfWriter(input));

        scheduledFuture = scheduler.schedule(new Runnable() {
                                                 @Override
                                                 public void run() {
                                                     reporter.report();
                                                 }
                                             },
                                             configuration.getInt(CK_REPORT_INTERVAL),
                                             TimeUnit.valueOf(configuration.getString(CK_REPORT_UNIT)));
    }

    @Override
    public void stop() {
        try {
            scheduledFuture.cancel(true);
        } catch (Exception ignored) {
        }
    }


    @Override
    public MetricSet getMetricSet() {
        return null;
    }

    @FactoryClass
    public interface Factory extends Transport.Factory<LocalMetricsTransport> {
        @Override
        LocalMetricsTransport create(Configuration configuration);

        @Override
        Config getConfig();
    }

    @ConfigClass
    public static class Config extends ThrottleableTransport.Config {
        @Override
        public ConfigurationRequest getRequestedConfiguration() {
            final ConfigurationRequest r = super.getRequestedConfiguration();

            r.addField(new TextField(
                    CK_SOURCE,
                    "Source",
                    "metrics",
                    "Define a name of the source. For example 'metrics'.",
                    ConfigurationField.Optional.NOT_OPTIONAL
            ));


            r.addField(
                    new NumberField(
                            CK_REPORT_INTERVAL,
                            "Report interval",
                            10,
                            "Time between each report. Select a time unit in the corresponding dropdown.",
                            ConfigurationField.Optional.NOT_OPTIONAL,
                            NumberField.Attribute.ONLY_POSITIVE
                    )
            );

            r.addField(
                    new DropdownField(
                            CK_REPORT_UNIT,
                            "Report interval unit",
                            TimeUnit.SECONDS.toString(),
                            DropdownField.ValueTemplates.timeUnits(),
                            ConfigurationField.Optional.NOT_OPTIONAL
                    )
            );

            r.addField(
                    new DropdownField(
                            CK_DURATION_UNIT,
                            "Time unit of measured durations",
                            TimeUnit.MILLISECONDS.toString(),
                            DropdownField.ValueTemplates.timeUnits(),
                            "The time unit that will be used in for example timer values. Think of: took 15ms",
                            ConfigurationField.Optional.NOT_OPTIONAL
                    )
            );

            r.addField(
                    new DropdownField(
                            CK_RATE_UNIT,
                            "Time unit of measured rates",
                            TimeUnit.SECONDS.toString(),
                            DropdownField.ValueTemplates.timeUnits(),
                            "The time unit that will be used in for example meter values. Think of: 7 per second",
                            ConfigurationField.Optional.NOT_OPTIONAL
                    )
            );

            return r;
        }
    }

    private class RawGelfWriter implements GELFTarget {
        private final MessageInput input;

        public RawGelfWriter(MessageInput input) {
            this.input = input;
        }

        @Override
        public void deliver(String shortMessage, String source, Map<String, Object> fields) {
            try {
                final Map<String, Object> data = Maps.newHashMap();
                data.put("short_message", shortMessage);
                data.put("host", source);
                data.putAll(fields);
                final byte[] payload = mapper.writeValueAsBytes(data);
                input.processRawMessage(new RawMessage("gelf", input.getId(), null, payload));
            } catch (JsonProcessingException e) {
                log.error("Unable to serialized metrics", e);
            }
        }
    }
}

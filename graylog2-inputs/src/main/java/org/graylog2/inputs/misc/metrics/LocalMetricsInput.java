/**
 * Copyright 2013 Lennart Koopmann <lennart@torch.sh>
 *
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
 *
 */
package org.graylog2.inputs.misc.metrics;

import org.graylog2.inputs.misc.metrics.agent.*;
import org.graylog2.plugin.GraylogServer;
import org.graylog2.plugin.InputHost;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationException;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.configuration.fields.DropdownField;
import org.graylog2.plugin.configuration.fields.NumberField;
import org.graylog2.plugin.configuration.fields.TextField;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.MisfireException;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class LocalMetricsInput extends MessageInput {

    public static final String NAME = "Internal metrics reporter";

    private Graylog2Reporter reporter;

    private static final String CK_REPORT_INTERVAL = "report_interval";
    private static final String CK_REPORT_UNIT = "report_unit";
    private static final String CK_DURATION_UNIT = "duration_unit";
    private static final String CK_RATE_UNIT = "rate_unit";
    private static final String CK_SOURCE = "source";

    @Override
    public void checkConfiguration() throws ConfigurationException {
        reporter = Graylog2Reporter.forRegistry(graylogServer.metrics())
                            .useSource(configuration.getString(CK_SOURCE))
                            .convertDurationsTo(TimeUnit.valueOf(configuration.getString(CK_DURATION_UNIT)))
                            .convertRatesTo(TimeUnit.valueOf(configuration.getString(CK_RATE_UNIT)))
                            .build(new InProcessMessageWriter(graylogServer, this));
    }

    @Override
    public void launch() throws MisfireException {
        reporter.start(
                configuration.getInt(CK_REPORT_INTERVAL),
                TimeUnit.valueOf(configuration.getString(CK_REPORT_UNIT))
        );
    }

    @Override
    public void stop() {
        reporter.stop();
    }

    @Override
    public ConfigurationRequest getRequestedConfiguration() {
        ConfigurationRequest r = new ConfigurationRequest();

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

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String linkToDocs() {
        return "";
    }

    @Override
    public Map<String, Object> getAttributes() {
        return configuration.getSource();
    }

    @Override
    public boolean isExclusive() {
        return true;
    }

}

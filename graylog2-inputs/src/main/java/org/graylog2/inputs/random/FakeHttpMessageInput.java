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
package org.graylog2.inputs.random;

import org.graylog2.inputs.random.generators.FakeHttpMessageGenerator;
import org.graylog2.inputs.random.generators.Tools;
import org.graylog2.plugin.InputHost;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationException;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.configuration.fields.NumberField;
import org.graylog2.plugin.configuration.fields.TextField;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.MisfireException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Random;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class FakeHttpMessageInput extends MessageInput {

    private static final Logger LOG = LoggerFactory.getLogger(FakeHttpMessageInput.class);

    public static final String NAME = "Random HTTP message generator";

    public static final String CK_SOURCE = "source";
    public static final String CK_SLEEP = "sleep";
    public static final String CK_SLEEP_DEVIATION_PERCENT = "sleep_deviation";

    private final Random rand = new Random();

    private boolean stopRequested = false;

    private String source;
    private int sleepMs;
    private int maxSleepDeviation;

    @Override
    public void checkConfiguration() throws ConfigurationException {
        if (!checkConfig(configuration)) {
            throw new ConfigurationException(configuration.getSource().toString());
        }

        source = configuration.getString(CK_SOURCE);
        sleepMs = (int) configuration.getInt(CK_SLEEP);
        maxSleepDeviation = (int) configuration.getInt(CK_SLEEP_DEVIATION_PERCENT);
    }

    @Override
    public void launch() throws MisfireException {
        FakeHttpMessageGenerator generator = new FakeHttpMessageGenerator(source);
        while(!stopRequested) {
            graylogServer.getProcessBuffer().insertCached(generator.generate(), this);

            try {
                Thread.sleep(Tools.deviation(sleepMs, maxSleepDeviation, rand));
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    @Override
    public void stop() {
        this.stopRequested = true;
    }

    @Override
    public ConfigurationRequest getRequestedConfiguration() {
        ConfigurationRequest c = new ConfigurationRequest();

        c.addField(new NumberField(
                CK_SLEEP,
                "Sleep time",
                25,
                "How many milliseconds to sleep between generating messages.",
                ConfigurationField.Optional.NOT_OPTIONAL,
                NumberField.Attribute.ONLY_POSITIVE
        ));

        c.addField(new NumberField(
                CK_SLEEP_DEVIATION_PERCENT,
                "Maximum random sleep time deviation",
                30,
                "The deviation is used to generate a more realistic and non-steady message flow.",
                ConfigurationField.Optional.NOT_OPTIONAL,
                NumberField.Attribute.ONLY_POSITIVE
        ));

        c.addField(new TextField(
                CK_SOURCE,
                "Source name",
                "example.org",
                "What to use as source of the generate messages.",
                ConfigurationField.Optional.NOT_OPTIONAL
        ));

        return c;
    }

    @Override
    public boolean isExclusive() {
        return false;
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

    private boolean checkConfig(Configuration config) {
        return config.stringIsSet(CK_SOURCE)
                && config.intIsSet(CK_SLEEP)
                && config.intIsSet(CK_SLEEP_DEVIATION_PERCENT);
    }

}

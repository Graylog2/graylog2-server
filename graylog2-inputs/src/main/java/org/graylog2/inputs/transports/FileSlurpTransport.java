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

import com.google.common.base.Charsets;
import com.google.common.eventbus.EventBus;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.apache.commons.io.IOUtils;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.configuration.fields.TextField;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.annotations.ConfigClass;
import org.graylog2.plugin.inputs.annotations.FactoryClass;
import org.graylog2.plugin.inputs.transports.GeneratorTransport;
import org.graylog2.plugin.inputs.transports.Transport;
import org.graylog2.plugin.journal.RawMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class FileSlurpTransport extends GeneratorTransport {
    private static final Logger log = LoggerFactory.getLogger(FileSlurpTransport.class);
    private final BufferedReader reader;

    @AssistedInject
    public FileSlurpTransport(EventBus eventBus, @Assisted Configuration configuration) {
        super(eventBus, configuration);
        final File file = new File(configuration.getString("file_path"));
        try {
            reader = new BufferedReader(new FileReader(file));
        } catch (FileNotFoundException e) {
            log.error("Cannot open file.", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    protected RawMessage produceRawMessage(MessageInput input) {
        try {
            final String line = reader.readLine();
            if (line == null) {
                IOUtils.closeQuietly(reader);
                throw new RuntimeException("EOF reached");
            }

            return new RawMessage(line.getBytes(Charsets.UTF_8));
        } catch (IOException e) {
            IOUtils.closeQuietly(reader);
            throw new RuntimeException(e);
        }
    }


    @FactoryClass
    public interface Factory extends Transport.Factory<FileSlurpTransport> {
        @Override
        FileSlurpTransport create(Configuration configuration);

        @Override
        Config getConfig();
    }

    @ConfigClass
    public static class Config extends GeneratorTransport.Config {

        @Override
        public ConfigurationRequest getRequestedConfiguration() {
            final ConfigurationRequest c = super.getRequestedConfiguration();
            c.addField(new TextField(
                    "file_path",
                    "Path to the input file",
                    "/tmp/rawinput.txt",
                    "A file to be read line-by-line as raw messages (for debugging only).",
                    ConfigurationField.Optional.OPTIONAL
            ));
            return c;
        }
    }
}

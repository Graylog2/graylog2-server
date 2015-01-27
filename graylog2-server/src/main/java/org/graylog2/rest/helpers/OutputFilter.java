package org.graylog2.rest.helpers;

import com.google.common.collect.Sets;
import org.graylog2.outputs.MessageOutputFactory;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.TextField;
import org.graylog2.plugin.outputs.MessageOutput;
import org.graylog2.plugin.outputs.MessageOutputConfigurationException;
import org.graylog2.rest.models.system.outputs.responses.OutputSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Map;
import java.util.Set;

public class OutputFilter {
    private static final Logger LOG = LoggerFactory.getLogger(OutputFilter.class);
    private static final String PASSWORD_ATTRIBUTE = TextField.Attribute.IS_PASSWORD.toString().toLowerCase();
    private final MessageOutputFactory messageOutputFactory;

    @Inject
    public OutputFilter(MessageOutputFactory messageOutputFactory) {
        this.messageOutputFactory = messageOutputFactory;
    }

    public Set<OutputSummary> filterPasswordFields(final Set<OutputSummary> outputs) {
        final Set<OutputSummary> data = Sets.newHashSet();

        for (OutputSummary output : outputs) {
            try {
                data.add(filterPasswordFields(output));
            } catch (MessageOutputConfigurationException e) {
                LOG.error("Unable to filter configuration fields for output {}: ", output.id(), e);
            }
        }

        return data;
    }

    // This is so ugly!
    // TODO: Remove this once we implemented proper types for input/output configuration.
    public OutputSummary filterPasswordFields(final OutputSummary output) throws MessageOutputConfigurationException {
        final Map<String, Object> data = output.configuration();
        final MessageOutput.Factory factory = messageOutputFactory.get(output.type());

        if (null == factory) {
            throw new MessageOutputConfigurationException("Couldn't find output of type " + output.type());
        }

        final ConfigurationRequest requestedConfiguration;
        try {
            requestedConfiguration = factory.getConfig().getRequestedConfiguration();
        } catch (Exception e) {
            throw new MessageOutputConfigurationException("Couldn't retrieve requested configuration for output " + output.title());
        }

        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if (requestedConfiguration.getField(entry.getKey()).getAttributes().contains(PASSWORD_ATTRIBUTE)) {
                data.put(entry.getKey(), "********");
            }
        }

        return OutputSummary.create(output.id(), output.title(), output.type(), output.creatorUserId(), output.createdAt(), data, output.contentPack());
    }
}

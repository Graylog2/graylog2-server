package org.graylog.plugins.cef.codec;

import com.google.common.base.Charsets;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog.plugins.cef.parser.CEFMessage;
import org.graylog.plugins.cef.parser.CEFParser;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.configuration.fields.TextField;
import org.graylog2.plugin.inputs.annotations.ConfigClass;
import org.graylog2.plugin.inputs.annotations.FactoryClass;
import org.graylog2.plugin.inputs.codecs.Codec;
import org.graylog2.plugin.inputs.codecs.CodecAggregator;
import org.graylog2.plugin.journal.RawMessage;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class CEFCodec extends BaseCEFCodec {
    public static final String NAME = "CEF";

    private final CEFParser parser;

    @AssistedInject
    public CEFCodec(@Assisted Configuration configuration) {
        super(configuration);

        this.parser = new CEFParser(useFullNames);
    }

    @Nullable
    @Override
    public Message decode(@Nonnull RawMessage rawMessage) {
        try {
            // CEF standard says all messages are UTF-8 so I trust that.
            String s = new String(rawMessage.getPayload(), Charsets.UTF_8);
            CEFMessage cef = parser.parse(s).build();

            // Build standard message.
            Message result = new Message(buildMessageSummary(cef), decideSource(cef, rawMessage), cef.timestamp());

            // Add all extensions.
            result.addFields(cef.fields());

            // Add standard CEF fields.
            result.addField("device_vendor", cef.deviceVendor());
            result.addField("device_product", cef.deviceProduct());
            result.addField("device_version", cef.deviceVersion());
            result.addField("event_class_id", cef.deviceEventClassId());
            result.addField("name", cef.name());
            result.addField("severity", cef.severity().text());
            result.addField("severity_number", cef.severity().numeric());

            // Add msg field if the CEF message has one.
            result.addField("msg", cef.message());

            return result;
        } catch (Exception e) {
            throw new RuntimeException("Could not decode CEF message.", e);
        }
    }

    @Override
    public String getName() {
        return NAME;
    }

    @FactoryClass
    public interface Factory extends Codec.Factory<CEFCodec> {
        @Override
        CEFCodec create(Configuration configuration);

        @Override
        Config getConfig();
    }

    @ConfigClass
    public static class Config extends BaseCEFCodec.Config {
    }
}

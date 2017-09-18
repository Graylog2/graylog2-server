package org.graylog.plugins.cef.codec;

import com.google.common.base.Charsets;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog.plugins.cef.parser.CEFMessage;
import org.graylog.plugins.cef.parser.SyslogCEFParser;
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

public class CEFSyslogCodec extends BaseCEFCodec {
    public static final String NAME = "CEF Syslog";

    private static final Logger LOG = LoggerFactory.getLogger(CEFSyslogCodec.class);
    private static final String CK_TIMEZONE = "timezone";

    private final SyslogCEFParser parser;

    @AssistedInject
    public CEFSyslogCodec(@Assisted Configuration configuration) {
        super(configuration);

        DateTimeZone timezone;
        try {
            timezone = DateTimeZone.forID(configuration.getString(CK_TIMEZONE));
        } catch (Exception e) {
            LOG.warn("Could not configure CEF input timezone. Falling back to local default. Please check the error message:", e);
            timezone = DateTimeZone.getDefault();
        }

        this.parser = new SyslogCEFParser(timezone);
    }

    @Nullable
    @Override
    public Message decode(@Nonnull RawMessage rawMessage) {
        try {
            // CEF standard says all messages are UTF-8 so I trust that.
            String s = new String(rawMessage.getPayload(), Charsets.UTF_8);
            CEFMessage cef = parser.parse(s);

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

    @Nullable
    @Override
    public CodecAggregator getAggregator() {
        return null;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Nonnull
    @Override
    public Configuration getConfiguration() {
        return configuration;
    }

    @FactoryClass
    public interface Factory extends Codec.Factory<CEFSyslogCodec> {
        @Override
        CEFSyslogCodec create(Configuration configuration);

        @Override
        Config getConfig();
    }

    @ConfigClass
    public static class Config implements Codec.Config {
        @Override
        public ConfigurationRequest getRequestedConfiguration() {
            ConfigurationRequest cr = new ConfigurationRequest();

            cr.addField(new TextField(
                    CK_TIMEZONE,
                    "Timezone",
                    DateTimeZone.getDefault().getID(),
                    "Timezone of the timestamps in the CEF messages we'l receive. Set this to the local timezone if in doubt. (CEF messages do not include timezone information) Format example: +01:00 or America/Chicago",
                    ConfigurationField.Optional.NOT_OPTIONAL
            ));

            return cr;
        }

        @Override
        public void overrideDefaultValues(@Nonnull ConfigurationRequest cr) {
        }
    }

}

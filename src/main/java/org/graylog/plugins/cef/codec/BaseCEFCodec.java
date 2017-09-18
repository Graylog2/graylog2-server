package org.graylog.plugins.cef.codec;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog.plugins.cef.parser.CEFMessage;
import org.graylog2.plugin.ResolvableInetSocketAddress;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.configuration.fields.TextField;
import org.graylog2.plugin.inputs.annotations.ConfigClass;
import org.graylog2.plugin.inputs.codecs.Codec;
import org.graylog2.plugin.inputs.codecs.CodecAggregator;
import org.graylog2.plugin.journal.RawMessage;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.net.InetSocketAddress;

public abstract class BaseCEFCodec implements Codec {
    private static final Logger LOG = LoggerFactory.getLogger(BaseCEFCodec.class);

    private static final String CK_TIMEZONE = "timezone";

    protected final Configuration configuration;

    @AssistedInject
    public BaseCEFCodec(@Assisted Configuration configuration) {
        this.configuration = configuration;

        DateTimeZone timezone;
        try {
            timezone = DateTimeZone.forID(configuration.getString(CK_TIMEZONE));
        } catch (Exception e) {
            LOG.warn("Could not configure CEF input timezone. Falling back to local default. Please check the error message:", e);
            timezone = DateTimeZone.getDefault();
        }
    }

    protected String buildMessageSummary(CEFMessage cef) {
        return cef.deviceProduct() + ": [" + cef.deviceEventClassId() + ", " + cef.humanReadableSeverity() + "] " + cef.name();
    }

    protected String decideSource(CEFMessage cef, RawMessage raw) {
        if (cef.fields() != null && cef.fields().containsKey("dvc")) {
            String dvc = (String) cef.fields().get("dvc");
            if (!dvc.isEmpty()) {
                return dvc;
            }
        }

        // Use raw message source information if we were not able to parse a source from the CEF extensions.
        final ResolvableInetSocketAddress address = raw.getRemoteAddress();
        final InetSocketAddress remoteAddress;
        if (address == null) {
            remoteAddress = null;
        } else {
            remoteAddress = address.getInetSocketAddress();
        }

        return remoteAddress == null ? "unknown" : remoteAddress.getAddress().toString();
    }

    @Nullable
    @Override
    public CodecAggregator getAggregator() {
        return null;
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

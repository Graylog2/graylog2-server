package org.graylog2.inputs.codecs;

import com.google.common.collect.Maps;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.jayway.jsonpath.JsonPath;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.configuration.fields.TextField;
import org.graylog2.plugin.inputs.codecs.Codec;
import org.graylog2.plugin.inputs.codecs.CodecAggregator;
import org.graylog2.plugin.journal.RawMessage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class JsonPathCodec implements Codec {

    public static final String CK_PATH = "path";
    public static final String CK_SOURCE = "source";

    private final JsonPath jsonPath;
    private final Configuration configuration;

    @AssistedInject
    public JsonPathCodec(@Assisted Configuration configuration) {
        this.configuration = configuration;
        if (configuration.stringIsSet(CK_PATH)) {
            jsonPath = JsonPath.compile(configuration.getString(CK_PATH));
        } else {
            jsonPath = null;
        }
    }

    @Nullable
    @Override
    public Message decode(@Nonnull RawMessage rawMessage) {
        if (jsonPath == null) {
            return null;
        }
        final String json = new String(rawMessage.getPayload(), StandardCharsets.UTF_8);
        final Object result = jsonPath.read(json);

        final Map<String, Object> fields = Maps.newHashMap();

        if (result instanceof Integer || result instanceof Double || result instanceof Long) {
            fields.put("result", result);
        } else if (result instanceof List) {
            final List list = (List) result;
            if (!list.isEmpty()) {
                fields.put("result", list.get(0).toString());
            }
        } else {
            // Now it's most likely a string or something we do not map.
            fields.put("result", result.toString());
        }

        final Message message = new Message(buildShortMessage(fields),
                                            configuration.getString(CK_SOURCE),
                                            rawMessage.getTimestamp());
        message.addFields(fields);
        return message;
    }

    private String buildShortMessage(Map<String, Object> fields) {
        final StringBuilder shortMessage = new StringBuilder();
        shortMessage.append("JSON API poll result: ");
        shortMessage.append(jsonPath.getPath()).append(" -> ");
        if (fields.toString().length() > 50) {
            shortMessage.append(fields.toString().substring(0, 50)).append("[...]");
        } else {
            shortMessage.append(fields.toString());
        }

        return shortMessage.toString();
    }

    @Nullable
    @Override
    public CodecAggregator getAggregator() {
        return null;
    }

    @Override
    public String getName() {
        return "JsonPath";
    }

    @Nonnull
    @Override
    public ConfigurationRequest getRequestedConfiguration() {
        final ConfigurationRequest r = new ConfigurationRequest();

        r.addField(new TextField(
                CK_PATH,
                "JSON path of data to extract",
                "$.store.book[1].number_of_orders",
                "Path to the value you want to extract from the JSON response. Take a look at the documentation for a more detailled explanation.",
                ConfigurationField.Optional.NOT_OPTIONAL
        ));

        r.addField(new TextField(
                CK_SOURCE,
                "Message source",
                "yourapi",
                "What to use as source field of the resulting message.",
                ConfigurationField.Optional.NOT_OPTIONAL
        ));

        return r;
    }

    @Override
    public void overrideDefaultValues(@Nonnull ConfigurationRequest cr) {
    }

    public interface Factory extends Codec.Factory<JsonPathCodec> {
        @Override
        JsonPathCodec create(Configuration configuration);
    }
}

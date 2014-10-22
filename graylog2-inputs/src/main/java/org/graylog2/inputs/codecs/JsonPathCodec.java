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
package org.graylog2.inputs.codecs;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.jayway.jsonpath.JsonPath;
import org.graylog2.plugin.ConfigClass;
import org.graylog2.plugin.FactoryClass;
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
        final Map<String, Object> fields = read(json);

        final Message message = new Message(buildShortMessage(fields),
                                            configuration.getString(CK_SOURCE),
                                            rawMessage.getTimestamp());
        message.addFields(fields);
        return message;
    }

    @VisibleForTesting
    protected Map<String, Object> read(String json) {
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
        return fields;
    }

    @VisibleForTesting
    protected String buildShortMessage(Map<String, Object> fields) {
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

    @FactoryClass
    public interface Factory extends Codec.Factory<JsonPathCodec> {
        @Override
        JsonPathCodec create(Configuration configuration);

        @Override
        Config getConfig();
    }

    @ConfigClass
    public static class Config implements Codec.Config {
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

            return r;        }

        @Override
        public void overrideDefaultValues(@Nonnull ConfigurationRequest cr) {

        }
    }
}

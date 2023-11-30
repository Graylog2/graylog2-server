package org.graylog2.inputs.codecs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.configuration.fields.TextField;
import org.graylog2.plugin.inputs.annotations.Codec;
import org.graylog2.plugin.inputs.annotations.ConfigClass;
import org.graylog2.plugin.inputs.annotations.FactoryClass;
import org.graylog2.plugin.inputs.codecs.AbstractCodec;
import org.graylog2.plugin.inputs.codecs.MultiMessageCodec;
import org.graylog2.plugin.journal.RawMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

import static com.google.common.base.Predicates.and;
import static com.google.common.base.Predicates.equalTo;
import static com.google.common.base.Predicates.not;
import static org.graylog2.plugin.Message.FIELD_ID;
import static org.graylog2.plugin.Message.FIELD_MESSAGE;
import static org.graylog2.plugin.Message.FIELD_SOURCE;
import static org.graylog2.plugin.Message.FIELD_TIMESTAMP;


@Codec(name = "resurface", displayName = "Resurface")
public class ResurfaceCodec extends AbstractCodec implements MultiMessageCodec {
    private static final Logger LOG = LoggerFactory.getLogger(ResurfaceCodec.class);
    public static final String CK_SOURCE = "source";

    @AssistedInject
    public ResurfaceCodec(@Assisted Configuration configuration) {
        super(configuration);
    }

    @Nullable
    @Override
    public Message decode(@Nonnull RawMessage rawMessage) {
        throw new UnsupportedOperationException("MultiMessageCodec " + getClass() + " does not support decode()");
    }

    @Nullable
    @Override
    public Collection<Message> decodeMessages(@Nonnull RawMessage rawMessage) {
        final String jsonMessageArray = new String(rawMessage.getPayload(), charset);

        final ArrayList<Map<String,Object>> resurfaceMessages;

        try {
             resurfaceMessages = new ObjectMapper().readValue(jsonMessageArray, new TypeReference<>() {});
        } catch (JsonProcessingException ex) {
            LOG.error("Unable to parse resurface message", ex);
            return Collections.emptyList();
        }



        return resurfaceMessages.stream().map(resurfaceMessage -> {
            resurfaceMessage.put(FIELD_MESSAGE, Maps.filterKeys(resurfaceMessage,
                    and(not(equalTo(FIELD_ID)), not(equalTo(FIELD_TIMESTAMP)))).toString());
            resurfaceMessage.put(FIELD_SOURCE, configuration.getString(CK_SOURCE));
//            resurfaceMessage.put(FIELD_TIMESTAMP, rawMessage.getTimestamp());
            return new Message(resurfaceMessage);
        }).collect(Collectors.toList());

//        return resurfaceMessages.stream().map(resurfaceMessage -> {
//            Message message = new Message(resurfaceMessage.toString(),
//                    configuration.getString(CK_SOURCE),
//                    rawMessage.getTimestamp());
//            message.addFields(resurfaceMessage);
//            return message;
//        }).collect(Collectors.toList());
    }

    @FactoryClass
    public interface Factory extends AbstractCodec.Factory<ResurfaceCodec> {
        @Override
        ResurfaceCodec create(Configuration configuration);

        @Override
        Config getConfig();

        @Override
        Descriptor getDescriptor();
    }

    @ConfigClass
    public static class Config extends AbstractCodec.Config {
        @Override
        public ConfigurationRequest getRequestedConfiguration() {
            final ConfigurationRequest r = super.getRequestedConfiguration();

            r.addField(new TextField(
                    CK_SOURCE,
                    "Message source",
                    "Resurface",
                    "What to use as source field of the resulting message.",
                    ConfigurationField.Optional.NOT_OPTIONAL
            ));

            return r;        }

        @Override
        public void overrideDefaultValues(@Nonnull ConfigurationRequest cr) {

        }
    }

    public static class Descriptor extends AbstractCodec.Descriptor {
        @Inject
        public Descriptor() {
            super(ResurfaceCodec.class.getAnnotation(Codec.class).displayName());
        }
    }
}

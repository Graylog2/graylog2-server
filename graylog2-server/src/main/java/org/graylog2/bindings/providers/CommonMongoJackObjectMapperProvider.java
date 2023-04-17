package org.graylog2.bindings.providers;

import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.graylog.plugins.views.search.views.MongoIgnore;
import org.graylog2.jackson.MongoJodaDateTimeDeserializer;
import org.graylog2.jackson.MongoJodaDateTimeSerializer;
import org.graylog2.jackson.MongoZonedDateTimeDeserializer;
import org.graylog2.jackson.MongoZonedDateTimeSerializer;
import org.graylog2.security.encryption.EncryptedValueMapperConfig;
import org.joda.time.DateTime;
import org.mongojack.internal.MongoJackModule;

import javax.inject.Inject;
import javax.inject.Provider;
import java.time.ZonedDateTime;

public class CommonMongoJackObjectMapperProvider implements Provider<ObjectMapper> {
    private final Provider<ObjectMapper> objectMapperProvider;

    private static final AnnotationIntrospector customAnnotationIntrospector = new JacksonAnnotationIntrospector() {
        @Override
        public boolean hasIgnoreMarker(final AnnotatedMember member) {
            return super.hasIgnoreMarker(member) || member.hasAnnotation(MongoIgnore.class);
        }
    };

    private static final com.fasterxml.jackson.databind.Module serializationModule = new SimpleModule("JSR-310-MongoJack")
            .addSerializer(ZonedDateTime.class, new MongoZonedDateTimeSerializer())
            .addDeserializer(ZonedDateTime.class, new MongoZonedDateTimeDeserializer())
            .addSerializer(DateTime.class, new MongoJodaDateTimeSerializer())
            .addDeserializer(DateTime.class, new MongoJodaDateTimeDeserializer());

    @Inject
    public CommonMongoJackObjectMapperProvider(Provider<ObjectMapper> objectMapperProvider) {
        this.objectMapperProvider = objectMapperProvider;
    }

    @Override
    public ObjectMapper get() {
        return configure(this.objectMapperProvider.get());
    }

    public static ObjectMapper configure(ObjectMapper objectMapper) {
        var configuredObjectMapper = objectMapper.copy()
                .setAnnotationIntrospector(customAnnotationIntrospector)
                .registerModule(serializationModule);

        MongoJackModule.configure(configuredObjectMapper);
        EncryptedValueMapperConfig.enableDatabase(configuredObjectMapper);

        return configuredObjectMapper;

    }
}

/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.shared.bindings.providers;

import com.codahale.metrics.json.MetricsModule;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.zafarkhaja.semver.Version;
import com.vdurmont.semver4j.Requirement;
import com.vdurmont.semver4j.Semver;
import org.graylog2.database.ObjectIdSerializer;
import org.graylog2.jackson.AutoValueSubtypeResolver;
import org.graylog2.jackson.JodaTimePeriodKeyDeserializer;
import org.graylog2.jackson.SemverDeserializer;
import org.graylog2.jackson.SemverRequirementDeserializer;
import org.graylog2.jackson.SemverRequirementSerializer;
import org.graylog2.jackson.SemverSerializer;
import org.graylog2.jackson.VersionDeserializer;
import org.graylog2.jackson.VersionSerializer;
import org.graylog2.plugin.inject.JacksonSubTypes;
import org.graylog2.shared.jackson.SizeSerializer;
import org.graylog2.shared.plugins.GraylogClassLoader;
import org.graylog2.shared.rest.RangeJsonSerializer;
import org.joda.time.Period;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Singleton
public class ObjectMapperProvider implements Provider<ObjectMapper> {
    protected final ObjectMapper objectMapper;

    public ObjectMapperProvider() {
        this(ObjectMapperProvider.class.getClassLoader(), Collections.emptySet());
    }

    @Inject
    public ObjectMapperProvider(@GraylogClassLoader final ClassLoader classLoader,
                                @JacksonSubTypes Set<NamedType> subtypes) {
        final ObjectMapper mapper = new ObjectMapper();
        final TypeFactory typeFactory = mapper.getTypeFactory().withClassLoader(classLoader);
        final AutoValueSubtypeResolver subtypeResolver = new AutoValueSubtypeResolver();

        this.objectMapper = mapper
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
                .disable(DeserializationFeature.FAIL_ON_MISSING_EXTERNAL_TYPE_ID_PROPERTY)
                .setPropertyNamingStrategy(new PropertyNamingStrategy.SnakeCaseStrategy())
                .setSubtypeResolver(subtypeResolver)
                .setTypeFactory(typeFactory)
                .registerModule(new GuavaModule())
                .registerModule(new JodaModule())
                .registerModule(new Jdk8Module())
                .registerModule(new JavaTimeModule())
                .registerModule(new MetricsModule(TimeUnit.SECONDS, TimeUnit.SECONDS, false))
                .registerModule(new SimpleModule("Graylog")
                        .addKeyDeserializer(Period.class, new JodaTimePeriodKeyDeserializer())
                        .addSerializer(new RangeJsonSerializer())
                        .addSerializer(new SizeSerializer())
                        .addSerializer(new ObjectIdSerializer())
                        .addSerializer(new VersionSerializer())
                        .addSerializer(new SemverSerializer())
                        .addSerializer(new SemverRequirementSerializer())
                        .addDeserializer(Version.class, new VersionDeserializer())
                        .addDeserializer(Semver.class, new SemverDeserializer())
                        .addDeserializer(Requirement.class, new SemverRequirementDeserializer())
                );

        if (subtypes != null) {
            objectMapper.registerSubtypes(subtypes.toArray(new NamedType[]{}));
        }
    }

    @Override
    public ObjectMapper get() {
        return objectMapper;
    }
}

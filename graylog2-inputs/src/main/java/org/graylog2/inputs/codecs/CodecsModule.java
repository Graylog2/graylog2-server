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
/*
 * Created by IntelliJ IDEA.
 * User: kroepke
 * Date: 07/10/14
 * Time: 12:39
 */
package org.graylog2.inputs.codecs;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.multibindings.MapBinder;
import org.graylog2.plugin.ConfigClass;
import org.graylog2.plugin.FactoryClass;
import org.graylog2.plugin.inputs.codecs.Codec;

import java.lang.annotation.Annotation;

public class CodecsModule extends AbstractModule {
    protected void configure() {
        final MapBinder<String, Codec.Factory<? extends Codec>> mapBinder =
                MapBinder.newMapBinder(binder(),
                                       TypeLiteral.get(String.class),
                                       new TypeLiteral<Codec.Factory<? extends Codec>>() {
                                       });

        installCodec(mapBinder, "raw", RawCodec.class);
        installCodec(mapBinder, "syslog", SyslogCodec.class);
        installCodec(mapBinder, "randomhttp", RandomHttpMessageCodec.class);
        installCodec(mapBinder, "gelf", GelfCodec.class);
        installCodec(mapBinder, "radio-msgpack", RadioMessageCodec.class);
        installCodec(mapBinder, "jsonpath", JsonPathCodec.class);
    }

    // TODO fix duplication with TransportsModule
    private void installCodec(
            MapBinder<String, Codec.Factory<? extends Codec>> mapBinder,
            String name,
            Class<? extends Codec> codecClass) {

        final Class<? extends Codec.Config> configClass =
                (Class<? extends Codec.Config>)
                        findInnerClassAnnotatedWith(ConfigClass.class, codecClass, Codec.Config.class);

        final Class<? extends Codec.Factory<? extends Codec>> factoryClass =
                (Class<? extends Codec.Factory<? extends Codec>>)
                        findInnerClassAnnotatedWith(FactoryClass.class, codecClass, Codec.Factory.class);

        if (configClass == null || factoryClass == null) {
            throw new IllegalStateException("Missing annotations on transport class " + codecClass);
        }
        installCodec(mapBinder, name, codecClass, configClass, factoryClass);
    }

    private void installCodec(
            MapBinder<String, Codec.Factory<? extends Codec>> mapBinder,
            String name,
            Class<? extends Codec> codecClass,
            Class<? extends Codec.Config> configClass,
            Class<? extends Codec.Factory<? extends Codec>> factoryClass) {

        final Key<? extends Codec.Factory<? extends Codec>> factoryKey = Key.get(factoryClass);

        install(new FactoryModuleBuilder()
                        .implement(Codec.class, codecClass)
                        .implement(Codec.Config.class, configClass)
                        .build(factoryClass));

        mapBinder.addBinding(name).to(factoryKey);
    }

    private Class<?> findInnerClassAnnotatedWith(Class<? extends Annotation> annotationClass,
                                                 Class<? extends Codec> codecClass,
                                                 Class<?> targetClass) {
        final Class<?>[] declaredClasses = codecClass.getDeclaredClasses();
        Class<?> annotatedClass = null;
        for (final Class<?> declaredClass : declaredClasses) {
            if (!declaredClass.isAnnotationPresent(annotationClass)) {
                continue;
            }
            // must be subclass of Transport.Config
            if (targetClass.isAssignableFrom(declaredClass)) {
                // TODO log error if configClass is already assigned
                annotatedClass = declaredClass;
            } else {
                // TODO log error and skip transport
            }
        }
        return annotatedClass;
    }
}

/**
 * The MIT License
 * Copyright (c) 2012 TORCH GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.graylog2.plugin.inject;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.multibindings.MapBinder;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.annotations.ConfigClass;
import org.graylog2.plugin.inputs.annotations.FactoryClass;
import org.graylog2.plugin.inputs.codecs.Codec;
import org.graylog2.plugin.inputs.transports.Transport;
import org.graylog2.plugin.outputs.MessageOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.lang.annotation.Annotation;

public abstract class Graylog2Module extends AbstractModule {
    private static final Logger LOG = LoggerFactory.getLogger(Graylog2Module.class);

    protected void installTransport(
            MapBinder<String, Transport.Factory<? extends Transport>> mapBinder,
            String name,
            Class<? extends Transport> transportClass) {

        final Class<? extends Transport.Config> configClass =
                (Class<? extends Transport.Config>)
                        findInnerClassAnnotatedWith(ConfigClass.class, transportClass, Transport.Config.class);

        final Class<? extends Transport.Factory<? extends Transport>> factoryClass =
                (Class<? extends Transport.Factory<? extends Transport>>)
                        findInnerClassAnnotatedWith(FactoryClass.class, transportClass, Transport.Factory.class);

        if (configClass == null) {
            LOG.error("Unable to find an inner class annotated with @ConfigClass in transport {}. This transport will not be available!",
                    transportClass);
            return;
        }
        if (factoryClass == null) {
            LOG.error("Unable to find an inner class annotated with @FactoryClass in transport {}. This transport will not be available!",
                    transportClass);
            return;
        }
        installTransport(mapBinder, name, transportClass, configClass, factoryClass);
    }

    protected void installTransport(
            MapBinder<String, Transport.Factory<? extends Transport>> mapBinder,
            String name,
            Class<? extends Transport> transportClass,
            Class<? extends Transport.Config> configClass,
            Class<? extends Transport.Factory<? extends Transport>> factoryClass) {
        final Key<? extends Transport.Factory<? extends Transport>> factoryKey = Key.get(factoryClass);
        install(new FactoryModuleBuilder()
                .implement(Transport.class, transportClass)
                .implement(Transport.Config.class, configClass)
                .build(factoryClass));

        mapBinder.addBinding(name).to(factoryKey);
    }

    protected void installCodec(MapBinder<String, Codec.Factory<? extends Codec>> mapBinder, Class<? extends Codec> codecClass) {
        if (codecClass.isAnnotationPresent(org.graylog2.plugin.inputs.annotations.Codec.class)) {
            final org.graylog2.plugin.inputs.annotations.Codec a = codecClass.getAnnotation(org.graylog2.plugin.inputs.annotations.Codec.class);
            installCodec(mapBinder, a.name(), codecClass);
        } else {
            LOG.error("Codec {} not annotated with {}. Cannot determine its id. This is a bug, please use that annotation, this codec will not be available",
                    codecClass, org.graylog2.plugin.inputs.annotations.Codec.class);
        }
    }

    protected void installCodec(
            MapBinder<String, Codec.Factory<? extends Codec>> mapBinder,
            String name,
            Class<? extends Codec> codecClass) {

        final Class<? extends Codec.Config> configClass =
                (Class<? extends Codec.Config>)
                        findInnerClassAnnotatedWith(ConfigClass.class, codecClass, Codec.Config.class);

        final Class<? extends Codec.Factory<? extends Codec>> factoryClass =
                (Class<? extends Codec.Factory<? extends Codec>>)
                        findInnerClassAnnotatedWith(FactoryClass.class, codecClass, Codec.Factory.class);

        if (configClass == null) {
            LOG.error("Unable to find an inner class annotated with @ConfigClass in codec {}. This codec will not be available!",
                    codecClass);
            return;
        }
        if (factoryClass == null) {
            LOG.error("Unable to find an inner class annotated with @FactoryClass in codec {}. This codec will not be available!",
                    codecClass);
            return;
        }
        installCodec(mapBinder, name, codecClass, configClass, factoryClass);
    }

    protected void installCodec(
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

    @Nullable
    protected Class<?> findInnerClassAnnotatedWith(Class<? extends Annotation> annotationClass,
                                                   Class<?> containingClass,
                                                   Class<?> targetClass) {
        final Class<?>[] declaredClasses = containingClass.getDeclaredClasses();
        Class<?> annotatedClass = null;
        for (final Class<?> declaredClass : declaredClasses) {
            if (!declaredClass.isAnnotationPresent(annotationClass)) {
                continue;
            }
            if (targetClass.isAssignableFrom(declaredClass)) {
                if (annotatedClass != null) {
                    LOG.error("Multiple annotations for {} found in {}. This is invalid.", annotatedClass.getSimpleName(), containingClass);
                    return null;
                }
                annotatedClass = declaredClass;
            } else {
                LOG.error("{} annotated as {} is not extending the expected {}. Did you forget to implement the correct interface?",
                        declaredClass, annotationClass.getSimpleName(), targetClass);
                return null;
            }
        }
        return annotatedClass;
    }

    protected MapBinder<String, Codec.Factory<? extends Codec>> codecMapBinder() {
        return MapBinder.newMapBinder(binder(),
                TypeLiteral.get(String.class),
                new TypeLiteral<Codec.Factory<? extends Codec>>() {
                });
    }

    protected MapBinder<String, Transport.Factory<? extends Transport>> transportMapBinder() {
        return MapBinder.newMapBinder(binder(),
                TypeLiteral.get(String.class),
                new TypeLiteral<Transport.Factory<? extends Transport>>() {
                });
    }

    protected MapBinder<String, MessageInput.Factory<? extends MessageInput>> inputsMapBinder() {
        return MapBinder.newMapBinder(binder(),
                TypeLiteral.get(String.class),
                new TypeLiteral<MessageInput.Factory<? extends MessageInput>>() {
                });
    }

    protected <T extends MessageInput> void installInput(MapBinder<String, MessageInput.Factory<? extends MessageInput>> inputMapBinder,
                                                         Class<T> target,
                                                         Class<? extends MessageInput.Factory<T>> targetFactory) {
        install(new FactoryModuleBuilder().implement(MessageInput.class, target).build(targetFactory));
        inputMapBinder.addBinding(target.getCanonicalName()).to(Key.get(targetFactory));
    }

    protected <T extends MessageInput> void installInput(MapBinder<String, MessageInput.Factory<? extends MessageInput>> inputMapBinder,
                                                         Class<T> target) {
        Class<? extends MessageInput.Factory<T>> factoryClass =
                (Class<? extends MessageInput.Factory<T>>) findInnerClassAnnotatedWith(FactoryClass.class, target, MessageInput.Factory.class);

        if (factoryClass == null) {
            LOG.error("Unable to find an inner class annotated with @FactoryClass in input {}. This input will not be available!", target);
            return;
        }

        installInput(inputMapBinder, target, factoryClass);
    }

    protected MapBinder<String, MessageOutput.Factory<? extends MessageOutput>> outputsMapBinder() {
        return MapBinder.newMapBinder(binder(),
                TypeLiteral.get(String.class),
                new TypeLiteral<MessageOutput.Factory<? extends MessageOutput>>() {
                });
    }

    protected <T extends MessageOutput> void installOutput(MapBinder<String, MessageOutput.Factory<? extends MessageOutput>> outputMapBinder,
                                                           Class<T> target,
                                                           Class<? extends MessageOutput.Factory<T>> targetFactory) {
        install(new FactoryModuleBuilder().implement(MessageOutput.class, target).build(targetFactory));
        outputMapBinder.addBinding(target.getCanonicalName()).to(Key.get(targetFactory));
    }

    protected <T extends MessageOutput> void installOutput(MapBinder<String, MessageOutput.Factory<? extends MessageOutput>> outputMapBinder,
                                                           Class<T> target) {
        Class<? extends MessageOutput.Factory<T>> factoryClass =
                (Class<? extends MessageOutput.Factory<T>>) findInnerClassAnnotatedWith(FactoryClass.class, target, MessageOutput.Factory.class);

        if (factoryClass == null) {
            LOG.error("Unable to find an inner class annotated with @FactoryClass in output {}. This output will not be available!", target);
            return;
        }

        installOutput(outputMapBinder, target, factoryClass);
    }
}

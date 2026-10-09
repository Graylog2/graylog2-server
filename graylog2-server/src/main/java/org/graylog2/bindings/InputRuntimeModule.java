package org.graylog2.bindings;

import com.google.inject.AbstractModule;
import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.multibindings.MapBinder;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.codecs.Codec;
import org.graylog2.plugin.inputs.transports.Transport;
import org.jspecify.annotations.NonNull;

import javax.annotation.Nullable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Objects;

/**
 * A guice module that binds the infrastructure components of the input system.
 * Individual inputs are bound outside of this, so each node can pick which ones it pulls in.
 */
public final class InputRuntimeModule extends AbstractModule {

    /**
     * Only binds types that are required by the input system.
     * Any concrete inputs, transports or codes must be in separate modules!
     */
    @Override
    protected void configure() {
        makeCodecBinder(binder());
        makeTransportBinder(binder());
        makeInputBinder(binder());
    }

    @NonNull
    public static MapBinder<String, Codec.Factory<? extends Codec>> makeCodecBinder(Binder binder) {
        return MapBinder.newMapBinder(binder,
                TypeLiteral.get(String.class),
                new TypeLiteral<>() {
                });
    }

    @NonNull
    public static MapBinder<String, Transport.Factory<? extends Transport>> makeTransportBinder(Binder binder) {
        return MapBinder.newMapBinder(binder,
                TypeLiteral.get(String.class),
                new TypeLiteral<>() {
                });
    }

    @NonNull
    public static MapBinder<String, MessageInput.Factory<? extends MessageInput>> makeInputBinder(Binder binder) {
        return MapBinder.newMapBinder(binder,
                TypeLiteral.get(String.class),
                new TypeLiteral<>() {
                });
    }

    public static <T extends MessageInput> void installInput(Binder binder,
                                                             MapBinder<String, MessageInput.Factory<? extends MessageInput>> inputMapBinder,
                                                             Class<T> target) {
        var targetFactory = findInputFactory(target);
        Objects.requireNonNull(targetFactory, "Could not find an input factory for " + target.getCanonicalName());

        binder.install(new FactoryModuleBuilder().implement(MessageInput.class, target).build(targetFactory));
        inputMapBinder.addBinding(target.getCanonicalName()).to(Key.get(targetFactory));
        bindFactoryProduct(binder, targetFactory, "getConfig", MessageInput.Config.class);
        bindFactoryProduct(binder, targetFactory, "getDescriptor", MessageInput.Descriptor.class);
    }

    @Nullable
    @SuppressWarnings("unchecked") // checked: the factory's type argument is target
    private static <T extends MessageInput> Class<? extends MessageInput.Factory<T>> findInputFactory(Class<T> target) {
        for (final Class<?> declaredClass : target.getDeclaredClasses()) {
            if (!MessageInput.Factory.class.isAssignableFrom(declaredClass)) {
                continue;
            }
            final Type factoryType = TypeLiteral.get(declaredClass).getSupertype(MessageInput.Factory.class).getType();
            if (factoryType instanceof ParameterizedType p && p.getActualTypeArguments()[0] == target) {
                return (Class<? extends MessageInput.Factory<T>>) declaredClass;
            }
        }
        return null;
    }

    // getMethod picks the override with the most specific return type, e.g. GELFTCPInput.Config
    private static void bindFactoryProduct(Binder binder, Class<?> factoryClass, String methodName, Class<?> baseType) {
        final Class<?> productType;
        try {
            productType = factoryClass.getMethod(methodName).getReturnType();
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e); // declared by MessageInput.Factory
        }
        if (productType != baseType) {
            binder.bind(productType);
        }
    }
}

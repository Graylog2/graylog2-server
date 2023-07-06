package org.graylog2.bindings;

import com.github.joschi.jadconfig.Parameter;
import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.util.Providers;
import org.reflections.ReflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.github.joschi.jadconfig.ReflectionUtils.getAllFields;
import static com.github.joschi.jadconfig.ReflectionUtils.getFieldValue;
import static com.google.inject.name.Names.named;
import static org.graylog2.shared.utilities.StringUtils.f;

public class NamedInjectConfigParametersModule extends AbstractModule {

    private static final Logger LOG = LoggerFactory.getLogger(NamedInjectConfigParametersModule.class);

    private final Set<Object> beans;
    private final boolean registerBeans;


    public NamedInjectConfigParametersModule(final Collection beans, final boolean registerBeans) {
        this.beans = new HashSet<Object>(beans);
        this.registerBeans = registerBeans;
    }

    public NamedInjectConfigParametersModule(final Collection beans) {
        this(beans, true);
    }

    @Override
    protected void configure() {
        if (registerBeans) {
            registerBeanInstances();
        }

        for (final Object bean : beans) {
            registerParameters(bean);
        }
    }

    @SuppressWarnings("unchecked")
    private void registerParameters(Object bean) {
        final Field[] fields = getAllFields(bean.getClass());
        Map<String, Method> methodOverrides = ReflectionUtils.getAllMethods(bean.getClass()).stream()
                .filter(obj -> Objects.nonNull(obj.getAnnotation(ParameterNamedInjectOverride.class)))
                .collect(Collectors.toMap(obj -> obj.getAnnotation(ParameterNamedInjectOverride.class).value(), Function.identity()));

        for (Field field : fields) {
            final Parameter parameter = field.getAnnotation(Parameter.class);

            if (parameter != null) {
                try {
                    final TypeLiteral typeLiteral = TypeLiteral.get(field.getGenericType());
                    Object value = getFieldValue(bean, field);
                    String parameterName = parameter.value();
                    if (methodOverrides.containsKey(parameterName)) {
                        Method method = methodOverrides.get(parameterName);
                        TypeLiteral<?> methodTypeLiteral = TypeLiteral.get(method.getGenericReturnType());
                        if (!typeLiteral.equals(methodTypeLiteral)) {
                            throw new IllegalStateException(f("Parameter %s type mismatch @%s#%s <> @%s#%s",
                                    parameterName,
                                    ParameterNamedInjectOverride.class.getSimpleName(),
                                    methodTypeLiteral,
                                    Parameter.class.getSimpleName(),
                                    typeLiteral
                            ));
                        }
                        value = ReflectionUtils.invoke(method, bean);
                    }

                    if (value == null) {
                        bind(typeLiteral).annotatedWith(named(parameterName)).toProvider(Providers.of(null));
                    } else {
                        bind(typeLiteral).annotatedWith(named(parameterName)).toInstance(value);
                    }
                } catch (IllegalAccessException e) {
                    LOG.warn("Couldn't bind \"" + field.getName() + "\"", e);
                }
            } else {
                LOG.debug("Skipping field {}", field.getName());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void registerBeanInstances() {
        for (final Object bean : beans) {
            final TypeLiteral typeLiteral = TypeLiteral.get(bean.getClass());
            bind(typeLiteral).toInstance(bean);
        }
    }
}

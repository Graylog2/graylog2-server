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
package org.graylog2.shared.bindings;

import com.google.common.base.Throwables;
import com.google.common.collect.Sets;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.Stage;
import com.google.inject.TypeLiteral;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

public class Hk2GuiceBridgeJitInjector implements InvocationHandler {
    private static Method getExistingBindingMethod;
    private static Method getBindingMethod;
    private static Method getTypeLiteralMethod;
    private static Method getRawTypeMethod;

    static {
        try {
            getExistingBindingMethod = Injector.class.getDeclaredMethod("getExistingBinding", Key.class);
            getBindingMethod = Injector.class.getDeclaredMethod("getBinding", Key.class);
            getTypeLiteralMethod = Key.class.getDeclaredMethod("getTypeLiteral");
            getRawTypeMethod = TypeLiteral.class.getDeclaredMethod("getRawType");
        } catch (NoSuchMethodException e) {
            throw Throwables.propagate(e);
        }
    }

    private final Set<String> packagePrefixes;
    private final Injector injector;

    public static Injector create(final Module module, final String... packagePrefixes) {
        return create(Guice.createInjector(Stage.PRODUCTION, module), packagePrefixes);
    }

    public static Injector create(final Iterable<? extends Module> modules, final String... packagePrefixes) {
        return create(Guice.createInjector(Stage.PRODUCTION, modules), packagePrefixes);
    }

    public static Injector create(final Injector injector, final String... packagePrefixes) {
        return (Injector) Proxy.newProxyInstance(
                Injector.class.getClassLoader(),
                new Class[]{Injector.class},
                new Hk2GuiceBridgeJitInjector(injector, packagePrefixes));
    }

    private Hk2GuiceBridgeJitInjector(final Injector injector, final String... packagePrefixes) {
        this.packagePrefixes = Sets.newHashSet(packagePrefixes);
        this.injector = checkNotNull(injector);
    }

    private boolean isInsideTargettedPackage(final Class<?> type) {
        final String packageName = type.getPackage().getName();
        for (String packagePrefix : packagePrefixes) {
            if (packageName.startsWith(packagePrefix)) {
                return true;
            }
        }

        return false;
    }

    /**
     * {@inheritDoc}
     */
    public Object invoke(final Object proxy, final Method method, final Object... args) throws Throwable {
        final Object result = method.invoke(injector, args);
        if (result == null && method.equals(getExistingBindingMethod)) {
            if (isInsideTargettedPackage((Class<?>) getRawTypeMethod.invoke(getTypeLiteralMethod.invoke(args[0])))) {
                return getBindingMethod.invoke(injector, args[0]);
            }
        }

        return result;
    }
}
package org.graylog2.bindings;

import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.Scope;
import com.google.inject.Scopes;

public class CustomScopes {
    /**
     * A scope for singletons which are meant to be lazily initialized. For instance, guice will initialize all
     * singletons it has bindings for, when entering {@link com.google.inject.Stage#PRODUCTION}. This scope will still
     * enable singleton semantics, but the singleton will only instantiated when it's really needed.
     */
    public static final Scope LAZY_SINGLETON = new Scope() {
        @Override
        public <T> Provider<T> scope(Key<T> key, Provider<T> unscoped) {
            return Scopes.SINGLETON.scope(key, unscoped);
        }

        @Override public String toString() {
            return "CustomScopes.LAZY_SINGLETON";
        }
    };
}

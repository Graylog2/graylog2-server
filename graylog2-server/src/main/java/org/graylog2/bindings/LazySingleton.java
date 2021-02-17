package org.graylog2.bindings;

import com.google.inject.ScopeAnnotation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Provides the same semantics as {@link com.google.inject.Singleton} but will delay initialization as much as
 * possible. See {@link CustomScopes#LAZY_SINGLETON}.
 */
@Target({ TYPE, METHOD })
@Retention(RUNTIME)
@ScopeAnnotation
public @interface LazySingleton {}

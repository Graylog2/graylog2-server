package org.graylog2.database;

import com.google.inject.BindingAnnotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Guice binding annotation for the set of registered sequence counter topic names.
 * Used with {@code Multibinder<String>} to allow modules (including plugins) to register
 * topic names for use with {@link MongoSequenceService}.
 */
@BindingAnnotation
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
public @interface SequenceTopics {}

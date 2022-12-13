package org.graylog.plugins.views.search.views;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Use this annotation to have additional properties in your DTOs that are not to be saved.
 * e.g. favorite in ViewDTO is a user depending property that can be "joined" in but should
 * never be persisted into the views collection.
 */
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface MongoIgnore {
}

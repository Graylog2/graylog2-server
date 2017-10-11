package org.graylog.plugins.enterprise.search;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method or field of a {@link SearchType search type} implementation as being overridable at execution time.
 *
 * This mechanism is to be used for settings that do not structurally alter the shape of a query, but influence its result
 * window. Typical candidates are pagination info, field selection and bucket sizes of aggregations.
 *
 * Properties marked with this annotation must have a sensible default value assigned to them, because not all executions
 * will pass ephemeral state, such as first time execution of pagination, headless interactions and upgrades of stored queries.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD})
public @interface ExecutionState {
}

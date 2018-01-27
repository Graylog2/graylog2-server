package org.graylog.plugins.map;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Can be used in conjunction with {@link ConditionalRunner} to disable tests if one or more resources doesn't exist.
 * <p>
 * Example:
 * <pre>{@code
 *    @literal @RunWith(ConditionalRunner.class)
 *    @literal @ResourceExistsCondition({"/file1.txt", "/file2.txt"})
 *     public class GeoIpResolverEngineTest {
 *        @literal @Test
 *        @literal @ResourceExistsCondition("/file3.txt")
 *         public void test() {
 *         }
 *     }
 * }</pre>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ResourceExistsCondition {
    /** List of resources that must exist to run the tests. */
    String[] value();
}


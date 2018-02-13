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


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
package org.graylog2.configuration;

import javax.inject.Qualifier;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Marks a {@link java.nio.file.Path} parameter
 * <p>Example usage:
 *
 * <pre>
 *   public class Config {
 *     &#064;Parameter("message_journal_dir")
 *     <b>&#064;GraylogDataDir</b>
 *     private Path messageJournalDir = Paths.get("journal");
 *   }
 *
 *   public class Journal {
 *     &#064;Inject
 *     Journal(<b>@GraylogDataDir("message_journal_dir")</b> Path journalDir) {
 *         // ...
 *     }
 *   }</pre>
 */
@Qualifier
@Documented
@Retention(RUNTIME)
public @interface GraylogDataDir {
    /** The path configuration name */
    String value() default "";
}

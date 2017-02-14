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
package org.graylog2.indexer.retention.strategies;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.plugin.indexer.retention.RetentionStrategyConfig;

/**
 * This is being used as the fallback {@link RetentionStrategyConfig} if the requested class is not
 * available (usually because it was contributed by a plugin which is not loaded).
 * <p>
 * By itself it does nothing useful except accepting all properties but not exposing them.
 */
@JsonAutoDetect
@AutoValue
@WithBeanGetter
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class UnknownRetentionStrategyConfig implements RetentionStrategyConfig {

    @JsonCreator
    public static UnknownRetentionStrategyConfig create() {
        return new AutoValue_UnknownRetentionStrategyConfig(UnknownRetentionStrategyConfig.class.getCanonicalName());
    }


    public static UnknownRetentionStrategyConfig createDefault() {
        return create();
    }
}

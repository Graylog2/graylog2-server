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
package org.graylog2.rest.models.system.loggers.responses;


import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;
import org.hibernate.validator.constraints.NotEmpty;
import org.joda.time.DateTime;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.util.Map;

@AutoValue
@WithBeanGetter
@JsonAutoDetect
public abstract class InternalLogMessage {
    @JsonProperty
    @NotEmpty
    public abstract String message();

    @JsonProperty("class_name")
    @NotEmpty
    public abstract String className();

    @JsonProperty
    @NotEmpty
    public abstract String level();

    @JsonProperty
    @Nullable
    public abstract String marker();

    @JsonProperty
    @NotNull
    public abstract DateTime timestamp();

    @JsonProperty
    @Nullable
    public abstract String throwable();

    @JsonProperty("thread_name")
    @NotEmpty
    public abstract String threadName();

    @JsonProperty
    @NotNull
    public abstract Map<String, String> context();

    @JsonCreator
    public static InternalLogMessage create(@JsonProperty("message") @NotEmpty String message,
                                            @JsonProperty("class_name") @NotEmpty String className,
                                            @JsonProperty("level") @NotEmpty String level,
                                            @JsonProperty("marker") @Nullable String marker,
                                            @JsonProperty("timestamp") @NotNull DateTime timestamp,
                                            @JsonProperty("throwable") @Nullable String throwable,
                                            @JsonProperty("thread_name") @NotEmpty String threadName,
                                            @JsonProperty("context") @NotNull Map<String, String> context) {
        return new AutoValue_InternalLogMessage(message, className, level, marker, timestamp, throwable, threadName, context);
    }

}

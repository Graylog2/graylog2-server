/**
 * This file is part of Graylog Pipeline Processor.
 *
 * Graylog Pipeline Processor is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog Pipeline Processor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog Pipeline Processor.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog.plugins.pipelineprocessor.ast;

import com.google.auto.value.AutoValue;
import com.google.common.collect.Sets;

import javax.annotation.Nullable;
import java.util.SortedSet;

@AutoValue
public abstract class Pipeline {

    @Nullable
    public abstract String id();
    public abstract String name();
    public abstract SortedSet<Stage> stages();

    public static Builder builder() {
        return new AutoValue_Pipeline.Builder();
    }

    public static Pipeline empty(String name) {
        return builder().name(name).stages(Sets.<Stage>newTreeSet()).build();
    }

    public abstract Builder toBuilder();

    public Pipeline withId(String id) {
        return toBuilder().id(id).build();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Pipeline build();

        public abstract Builder id(String id);

        public abstract Builder name(String name);

        public abstract Builder stages(SortedSet<Stage> stages);
    }

    public String toString() {
        final StringBuilder sb = new StringBuilder("Pipeline ");
        sb.append("'").append(name()).append("'");
        sb.append(" (").append(id()).append(")");
        return sb.toString();
    }
}

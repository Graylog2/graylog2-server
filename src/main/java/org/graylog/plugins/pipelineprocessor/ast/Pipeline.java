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

import java.util.List;

@AutoValue
public abstract class Pipeline {

    public abstract String name();
    public abstract List<Stage> stages();

    public static Builder builder() {
        return new AutoValue_Pipeline.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Pipeline build();

        public abstract Builder name(String name);

        public abstract Builder stages(List<Stage> stages);
    }
}

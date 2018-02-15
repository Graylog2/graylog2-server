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
package org.graylog.plugins.pipelineprocessor.rest;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.plugins.pipelineprocessor.ast.Pipeline;
import org.graylog.plugins.pipelineprocessor.db.PipelineDao;
import org.graylog.plugins.pipelineprocessor.parser.ParseException;
import org.graylog.plugins.pipelineprocessor.parser.PipelineRuleParser;
import org.graylog.plugins.pipelineprocessor.parser.errors.ParseError;
import org.joda.time.DateTime;
import org.mongojack.Id;
import org.mongojack.ObjectId;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@AutoValue
@JsonAutoDetect
public abstract class PipelineSource {

    @JsonProperty("id")
    @Nullable
    @Id
    @ObjectId
    public abstract String id();

    @JsonProperty("title")
    @Nullable
    public abstract String title();

    @JsonProperty("description")
    @Nullable
    public abstract String description();

    @JsonProperty("source")
    public abstract String source();

    @JsonProperty("created_at")
    @Nullable
    public abstract DateTime createdAt();

    @JsonProperty("modified_at")
    @Nullable
    public abstract DateTime modifiedAt();

    @JsonProperty("stages")
    public abstract List<StageSource> stages();

    @JsonProperty("errors")
    @Nullable
    public abstract Set<ParseError> errors();

    public static Builder builder() {
        return new AutoValue_PipelineSource.Builder();
    }

    public abstract Builder toBuilder();

    @JsonCreator
    public static PipelineSource create(@JsonProperty("id") @Id @ObjectId @Nullable String id,
                                        @JsonProperty("title") String title,
                                        @JsonProperty("description") @Nullable String description,
                                        @JsonProperty("source") String source,
                                        @Nullable @JsonProperty("stages") List<StageSource> stages,
                                        @Nullable @JsonProperty("created_at") DateTime createdAt,
                                        @Nullable @JsonProperty("modified_at") DateTime modifiedAt) {
        return builder()
                .id(id)
                .title(title)
                .description(description)
                .source(source)
                .createdAt(createdAt)
                .modifiedAt(modifiedAt)
                .stages(stages == null ? Collections.emptyList() : stages)
                .build();
    }

    public static PipelineSource fromDao(PipelineRuleParser parser, PipelineDao dao) {
        Set<ParseError> errors = null;
        Pipeline pipeline = null;
        try {
            pipeline = parser.parsePipeline(dao.id(), dao.source());
        } catch (ParseException e) {
            errors = e.getErrors();
        }
        final List<StageSource> stageSources = (pipeline == null) ? Collections.emptyList() :
                pipeline.stages().stream()
                        .map(stage -> StageSource.builder()
                                .matchAll(stage.matchAll())
                                .rules(stage.ruleReferences())
                                .stage(stage.stage())
                                .build())
                        .collect(Collectors.toList());

        return builder()
                .id(dao.id())
                .title(dao.title())
                .description(dao.description())
                .source(dao.source())
                .createdAt(dao.createdAt())
                .modifiedAt(dao.modifiedAt())
                .stages(stageSources)
                .errors(errors)
                .build();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract PipelineSource build();

        public abstract Builder id(String id);

        public abstract Builder title(String title);

        public abstract Builder description(String description);

        public abstract Builder source(String source);

        public abstract Builder createdAt(DateTime createdAt);

        public abstract Builder modifiedAt(DateTime modifiedAt);

        public abstract Builder stages(List<StageSource> stages);

        public abstract Builder errors(Set<ParseError> errors);
    }
}

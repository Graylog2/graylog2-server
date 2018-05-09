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
package org.graylog2.contentpacks.codecs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.ModelTypes;
import org.graylog2.contentpacks.model.entities.Entity;
import org.graylog2.contentpacks.model.entities.EntityExcerpt;
import org.graylog2.contentpacks.model.entities.EntityV1;
import org.graylog2.contentpacks.model.entities.EntityWithConstraints;
import org.graylog2.contentpacks.model.entities.StreamEntity;
import org.graylog2.contentpacks.model.entities.StreamRuleEntity;
import org.graylog2.indexer.indexset.IndexSetService;
import org.graylog2.plugin.streams.Output;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.plugin.streams.StreamRule;
import org.graylog2.rest.resources.streams.requests.CreateStreamRequest;
import org.graylog2.rest.resources.streams.rules.requests.CreateStreamRuleRequest;
import org.graylog2.streams.StreamRuleService;
import org.graylog2.streams.StreamService;

import javax.inject.Inject;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class StreamCodec implements EntityCodec<Stream> {
    private final ObjectMapper objectMapper;
    private final StreamService streamService;
    private final StreamRuleService streamRuleService;
    private final IndexSetService indexSetService;

    @Inject
    public StreamCodec(ObjectMapper objectMapper,
                       StreamService streamService,
                       StreamRuleService streamRuleService,
                       IndexSetService indexSetService) {
        this.objectMapper = objectMapper;
        this.streamService = streamService;
        this.streamRuleService = streamRuleService;
        this.indexSetService = indexSetService;
    }

    @Override
    public EntityWithConstraints encode(Stream stream) {
        final List<StreamRuleEntity> streamRules = stream.getStreamRules().stream()
                .map(this::encodeStreamRule)
                .collect(Collectors.toList());
        final Set<String> outputIds = stream.getOutputs().stream()
                .map(Output::getId)
                .collect(Collectors.toSet());
        final StreamEntity streamEntity = StreamEntity.create(
                stream.getTitle(),
                stream.getDescription(),
                stream.getDisabled(),
                stream.getMatchingType(),
                streamRules,
                outputIds,
                stream.isDefaultStream(),
                stream.getRemoveMatchesFromDefaultStream());

        final JsonNode data = objectMapper.convertValue(streamEntity, JsonNode.class);
        final EntityV1 entity = EntityV1.builder()
                .id(ModelId.of(stream.getId()))
                .type(ModelTypes.STREAM)
                .data(data)
                .build();
        return EntityWithConstraints.create(entity);
    }

    private StreamRuleEntity encodeStreamRule(StreamRule streamRule) {
        return StreamRuleEntity.create(
                streamRule.getType(),
                streamRule.getField(),
                streamRule.getValue(),
                streamRule.getInverted(),
                streamRule.getDescription());
    }

    @Override
    public Stream decode(Entity entity) {
        if (entity instanceof EntityV1) {
            return decodeEntityV1((EntityV1) entity);
        } else {
            throw new IllegalArgumentException("Unsupported entity version: " + entity.getClass());

        }
    }

    private Stream decodeEntityV1(EntityV1 entity) {
        final StreamEntity streamEntity = objectMapper.convertValue(entity.data(), StreamEntity.class);
        final CreateStreamRequest createStreamRequest = CreateStreamRequest.create(
                streamEntity.title(),
                streamEntity.description(),
                null, // ignored
                null,
                streamEntity.matchingType().name(),
                streamEntity.removeMatches(),
                indexSetService.getDefault().id());

        // TODO: Pass along user
        final Stream stream = streamService.create(createStreamRequest, "admin");

        for (StreamRuleEntity streamRuleEntity : streamEntity.streamRules()) {
            final CreateStreamRuleRequest createStreamRuleRequest = CreateStreamRuleRequest.create(
                    streamRuleEntity.type().getValue(),
                    streamRuleEntity.value(),
                    streamRuleEntity.field(),
                    streamRuleEntity.inverted(),
                    streamRuleEntity.description());
            streamRuleService.create(stream.getId(), createStreamRuleRequest);
        }

        // TODO: Assign Stream outputs

        return stream;
    }


    @Override
    public EntityExcerpt createExcerpt(Stream stream) {
        return EntityExcerpt.builder()
                .id(ModelId.of(stream.getId()))
                .type(ModelTypes.STREAM)
                .title(stream.getTitle())
                .build();
    }
}

package org.graylog2.contentpacks.facades;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.graph.Graph;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.ImmutableGraph;
import com.google.common.graph.MutableGraph;
import org.graylog.events.legacy.V20190722150700_LegacyAlertConditionMigration;
import org.graylog2.contentpacks.exceptions.ContentPackException;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.ModelType;
import org.graylog2.contentpacks.model.ModelTypes;
import org.graylog2.contentpacks.model.entities.EntityDescriptor;
import org.graylog2.database.NotFoundException;
import org.graylog2.indexer.indexset.IndexSetService;
import org.graylog2.plugin.streams.Output;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.shared.users.UserService;
import org.graylog2.streams.StreamRuleService;
import org.graylog2.streams.StreamService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Map;

public class StreamTitleFacade extends StreamFacade {
    private static final Logger LOG = LoggerFactory.getLogger(StreamTitleFacade.class);

    public static final ModelType TYPE_V1 = ModelTypes.STREAM_TITLE;
    private final StreamService streamService;

    @Inject
    public StreamTitleFacade(ObjectMapper objectMapper, StreamService streamService, StreamRuleService streamRuleService, V20190722150700_LegacyAlertConditionMigration legacyAlertsMigration, IndexSetService indexSetService, UserService userService) {
        super(objectMapper, streamService, streamRuleService, legacyAlertsMigration, indexSetService, userService);
        this.streamService = streamService;
    }

    @Override
    public Graph<EntityDescriptor> resolveNativeEntity(EntityDescriptor entityDescriptor) {
        final MutableGraph<EntityDescriptor> mutableGraph = GraphBuilder.directed().build();
//        try {
//            final Stream stream = streamService.load(entityDescriptor.id().id());
//            mutableGraph.addNode(EntityDescriptor.create(entityDescriptor.id().id(), ModelTypes.STREAM_TITLE));
//        } catch (NotFoundException e) {
//            LOG.debug("Couldn't find stream {}", entityDescriptor, e);
//        }
        mutableGraph.addNode(EntityDescriptor.create(entityDescriptor.id().id(), ModelTypes.STREAM_TITLE));
        return ImmutableGraph.copyOf(mutableGraph);
    }

    @Override
    public String resolveEntityDescriptorId(EntityDescriptor entityDescriptor) {
        try {
            final Stream stream = streamService.load(entityDescriptor.id().id());
            return stream.getTitle();
        } catch (NotFoundException e) {
            throw new ContentPackException();
        }
    }

//    @Override
//    public void resolveNativeEntity(EntityDescriptor entityDescriptor, MutableGraph<EntityDescriptor> mutableGraph) {
//        streams().forEach(streamId -> {
//            final EntityDescriptor depStream = EntityDescriptor.builder()
//                    .id(ModelId.of(streamId))
//                    .type(ModelTypes.STREAM_TITLE)
//                    .build();
//            mutableGraph.putEdge(entityDescriptor, depStream);
//        });
//    }
}

package org.graylog.plugins.enterprise.search.rest;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.database.NotFoundException;
import org.graylog2.indexer.fieldtypes.FieldTypeDTO;
import org.graylog2.indexer.fieldtypes.FieldTypeMapper;
import org.graylog2.indexer.fieldtypes.FieldTypes;
import org.graylog2.indexer.fieldtypes.IndexFieldTypesDTO;
import org.graylog2.indexer.fieldtypes.IndexFieldTypesService;
import org.graylog2.plugin.rest.PluginRestResource;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;
import org.graylog2.streams.StreamService;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.collect.ImmutableSet.of;
import static org.graylog2.indexer.fieldtypes.FieldTypes.Type.createType;

@Api(value = "Enterprise/Field Types", description = "Field Types")
@Path("/fields")
@Produces(MediaType.APPLICATION_JSON)
@RequiresAuthentication
public class FieldTypesResource extends RestResource implements PluginRestResource {
    private final IndexFieldTypesService indexFieldTypesService;
    private final StreamService streamService;
    private final FieldTypeMapper fieldTypeMapper;
    private static final FieldTypes.Type UNKNOWN_TYPE = createType("unknown", of());
    private static final String PROP_COMPOUND_TYPE = "compound";

    @Inject
    public FieldTypesResource(IndexFieldTypesService indexFieldTypesService, StreamService streamService, FieldTypeMapper fieldTypeMapper) {
        this.indexFieldTypesService = indexFieldTypesService;
        this.streamService = streamService;
        this.fieldTypeMapper = fieldTypeMapper;
    }

    private Set<MappedFieldTypeDTO> mergeCompoundFieldTypes(java.util.stream.Stream<MappedFieldTypeDTO> stream) {
        return stream.collect(Collectors.groupingBy(MappedFieldTypeDTO::name, Collectors.toSet()))
                .entrySet()
                .stream()
                .map(entry -> {
                    final Set<MappedFieldTypeDTO> fieldTypes = entry.getValue();
                    final String fieldName = entry.getKey();
                    if (fieldTypes.size() == 1) {
                        return fieldTypes.iterator().next();
                    }
                    final String compoundFieldType = "compound(" + fieldTypes.stream()
                            .map(mappedFieldTypeDTO -> mappedFieldTypeDTO.type().type())
                            .collect(Collectors.joining(",")) + ")";
                    final ImmutableSet<String> commonProperties = fieldTypes.stream()
                            .map(mappedFieldTypeDTO -> mappedFieldTypeDTO.type().properties())
                            .reduce((s1, s2) -> Sets.intersection(s1, s2).immutableCopy())
                            .orElse(ImmutableSet.of());

                    final ImmutableSet<String> properties = ImmutableSet.<String>builder().addAll(commonProperties).add(PROP_COMPOUND_TYPE).build();
                    return MappedFieldTypeDTO.create(fieldName, createType(compoundFieldType, properties));
                })
                .collect(Collectors.toSet());

    }

    private MappedFieldTypeDTO mapPhysicalFieldType(FieldTypeDTO fieldType) {
        final FieldTypes.Type mappedFieldType = fieldTypeMapper.mapType(fieldType.physicalType()).orElse(UNKNOWN_TYPE);
        return MappedFieldTypeDTO.create(fieldType.fieldName(), mappedFieldType);
    }

    @GET
    @ApiOperation(value = "Retrieve the list of all fields present in the system")
    public Set<MappedFieldTypeDTO> allFieldTypes() {
        if (allowedToReadStream("*")) {
            return mergeCompoundFieldTypes(indexFieldTypesService.findAll()
                    .stream()
                    .map(IndexFieldTypesDTO::fields)
                    .flatMap(Collection::stream)
                    .map(this::mapPhysicalFieldType));
        }
        final Set<String> allowedStreams = streamService.loadAll()
                .stream()
                .map(Stream::getId)
                .filter(this::allowedToReadStream)
                .collect(Collectors.toSet());

        return fieldTypesByStreamIds(allowedStreams);
    }

    private boolean allowedToReadStream(String streamId) {
        return isPermitted(RestPermissions.STREAMS_READ, streamId);
    }

    @POST
    @ApiOperation(value = "Retrieve the field list of a given set of streams")
    @NoAuditEvent("This is not changing any data")
    public Set<MappedFieldTypeDTO> byStreams(FieldTypesForStreamsRequest request) {
        request.streams().forEach(s -> checkPermission(RestPermissions.STREAMS_READ, s));

        return fieldTypesByStreamIds(request.streams());
    }

    private Set<MappedFieldTypeDTO> fieldTypesByStreamIds(Set<String> streamIds) {
        return mergeCompoundFieldTypes(
                streamIds
                        .stream()
                        .map(streamId -> {
                            try {
                                return streamService.load(streamId);
                            } catch (NotFoundException e) {
                                throw new RuntimeException(e);
                            }
                        })
                        .filter(Objects::nonNull)
                        .map(indexSet -> indexSet.getIndexSet().getConfig().id())
                        .flatMap(indexSetId -> this.indexFieldTypesService.findForIndexSet(indexSetId).stream())
                        .map(IndexFieldTypesDTO::fields)
                        .flatMap(Collection::stream)
                        .map(this::mapPhysicalFieldType)
        );
    }
}

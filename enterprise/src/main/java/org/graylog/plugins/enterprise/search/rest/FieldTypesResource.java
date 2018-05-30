package org.graylog.plugins.enterprise.search.rest;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import io.swagger.annotations.Api;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.database.NotFoundException;
import org.graylog2.indexer.fieldtypes.FieldTypeDTO;
import org.graylog2.indexer.fieldtypes.FieldTypeMapper;
import org.graylog2.indexer.fieldtypes.FieldTypes;
import org.graylog2.indexer.fieldtypes.IndexFieldTypesDTO;
import org.graylog2.indexer.fieldtypes.IndexFieldTypesService;
import org.graylog2.plugin.rest.PluginRestResource;
import org.graylog2.shared.rest.resources.RestResource;
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
import java.util.stream.Stream;

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

    @Inject
    public FieldTypesResource(IndexFieldTypesService indexFieldTypesService, StreamService streamService, FieldTypeMapper fieldTypeMapper) {
        this.indexFieldTypesService = indexFieldTypesService;
        this.streamService = streamService;
        this.fieldTypeMapper = fieldTypeMapper;
    }

    private Set<MappedFieldTypeDTO> mergeCompoundFieldTypes(Stream<MappedFieldTypeDTO> stream) {
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
                    return MappedFieldTypeDTO.create(fieldName, createType(compoundFieldType, commonProperties));
                })
                .collect(Collectors.toSet());

    }

    private MappedFieldTypeDTO mapPhysicalFieldType(FieldTypeDTO fieldType) {
        final FieldTypes.Type mappedFieldType = fieldTypeMapper.mapType(fieldType.physicalType()).orElse(UNKNOWN_TYPE);
        return MappedFieldTypeDTO.create(fieldType.fieldName(), mappedFieldType);
    }

    @GET
    public Set<MappedFieldTypeDTO> allFieldTypes() {
        return mergeCompoundFieldTypes(indexFieldTypesService.findAll()
                .stream()
                .map(IndexFieldTypesDTO::fields)
                .flatMap(Collection::stream)
                .map(this::mapPhysicalFieldType));
    }

    @POST
    public Set<MappedFieldTypeDTO> byStreams(FieldTypesForStreamsRequest request) {
        return mergeCompoundFieldTypes(request.streams()
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
                .map(this::mapPhysicalFieldType));
    }
}

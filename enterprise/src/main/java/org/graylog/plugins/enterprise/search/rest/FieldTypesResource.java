package org.graylog.plugins.enterprise.search.rest;

import io.swagger.annotations.Api;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.database.NotFoundException;
import org.graylog2.indexer.fieldtypes.FieldTypeDTO;
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

@Api(value = "Enterprise/Field Types", description = "Field Types")
@Path("/fields")
@Produces(MediaType.APPLICATION_JSON)
@RequiresAuthentication
public class FieldTypesResource extends RestResource implements PluginRestResource {
    private final IndexFieldTypesService indexFieldTypesService;
    private final StreamService streamService;

    @Inject
    public FieldTypesResource(IndexFieldTypesService indexFieldTypesService, StreamService streamService) {
        this.indexFieldTypesService = indexFieldTypesService;
        this.streamService = streamService;
    }

    private Set<FieldTypeDTO> mergeCompoundFieldTypes(Stream<FieldTypeDTO> stream) {
        return stream.collect(Collectors.groupingBy(FieldTypeDTO::fieldName, Collectors.toSet()))
                .entrySet()
                .stream()
                .map(entry -> {
                    final Set<FieldTypeDTO> fieldTypes = entry.getValue();
                    final String fieldName = entry.getKey();
                    if (fieldTypes.size() == 1) {
                        return fieldTypes.iterator().next();
                    }
                    final String compoundFieldType = "compound(" + fieldTypes.stream().map(FieldTypeDTO::physicalType).collect(Collectors.joining(",")) + ")";
                    return FieldTypeDTO.create(fieldName, compoundFieldType);
                })
                .collect(Collectors.toSet());

    }

    @GET
    public Set<FieldTypeDTO> allFieldTypes() {
        return mergeCompoundFieldTypes(indexFieldTypesService.streamAll()
                .map(IndexFieldTypesDTO::fields)
                .flatMap(Collection::stream));
    }

    @POST
    public Set<FieldTypeDTO> byStreams(FieldTypesForStreamsRequest request) {
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
                .flatMap(this.indexFieldTypesService::streamForIndexSet)
                .map(IndexFieldTypesDTO::fields)
                .flatMap(Collection::stream));
    }
}

package org.graylog.plugins.views.search.rest;

import com.google.common.collect.ImmutableSet;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog.plugins.views.search.engine.QueryEngine;
import org.graylog.plugins.views.search.engine.ValidationExplanation;
import org.graylog.plugins.views.search.engine.ValidationRequest;
import org.graylog.plugins.views.search.engine.ValidationResponse;
import org.graylog2.plugin.indexer.searches.timeranges.InvalidRangeParametersException;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.graylog2.plugin.rest.PluginRestResource;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;

import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@RequiresAuthentication
@Api(value = "Search/Validation")
@Path("/search/validation")
public class QueryValidationResource extends RestResource implements PluginRestResource {

    private final QueryEngine queryEngine;
    private final PermittedStreams permittedStreams;

    @Inject
    public QueryValidationResource(QueryEngine queryEngine, PermittedStreams permittedStreams) {
        this.queryEngine = queryEngine;
        this.permittedStreams = permittedStreams;
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Validate a search query")
    public ValidationResponseDTO validateQuery(@ApiParam(name = "validationRequest") ValidationRequestDTO validationRequest) {

        final ValidationRequest q = ValidationRequest.Builder.builder()
                .query(validationRequest.query())
                .timerange(Optional.ofNullable(validationRequest.timerange()).orElse(defaultTimeRange()))
                .streams(adaptStreams(validationRequest.streams()))
                .build();

        final ValidationResponse response = queryEngine.validate(q);

        return new ValidationResponseDTO(response.isValid(), toExplanations(response.getExplanations()));
    }

    private List<ValidationExplanationDTO> toExplanations(List<ValidationExplanation> explanations) {
        return explanations.stream()
                .map(e -> new ValidationExplanationDTO(e.getIndex(), e.isValid(), e.getExplanation(), e.getError()))
                .collect(Collectors.toList());
    }

    private Set<String> adaptStreams(Set<String> streams) {
        if (streams == null || streams.isEmpty()) {
            return loadAllAllowedStreamsForUser();
        } else {
            // TODO: is it ok to filter out a stream that's not accessible or should we throw an exception?
            return streams.stream().filter(this::hasStreamReadPermission).collect(Collectors.toSet());
        }
    }

    private RelativeRange defaultTimeRange() {
        try {
            return RelativeRange.create(300);
        } catch (InvalidRangeParametersException e) {
            throw new RuntimeException(e);
        }
    }

    private ImmutableSet<String> loadAllAllowedStreamsForUser() {
        return permittedStreams.load(this::hasStreamReadPermission);
    }

    private boolean hasStreamReadPermission(String streamId) {
        return isPermitted(RestPermissions.STREAMS_READ, streamId);
    }

    protected boolean isPermitted(String permission, String instanceId) {
        return getSubject().isPermitted(permission + ":" + instanceId);
    }
}

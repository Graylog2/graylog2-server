/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog2.rest.resources.system.inputs;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog.plugins.pipelineprocessor.db.PipelineInputsMetadataDao;
import org.graylog.plugins.pipelineprocessor.db.PipelineService;
import org.graylog.plugins.pipelineprocessor.db.mongodb.MongoDbInputsMetadataService;
import org.graylog.plugins.pipelineprocessor.rest.PipelineRestPermissions;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog2.Configuration;
import org.graylog2.audit.AuditEventTypes;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.database.PaginatedList;
import org.graylog2.database.filtering.ComputedFieldRegistry;
import org.graylog2.database.filtering.DbQueryCreator;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.inputs.Input;
import org.graylog2.inputs.InputImpl;
import org.graylog2.inputs.InputRuntimeStatusProvider;
import org.graylog2.inputs.InputService;
import org.graylog2.inputs.diagnosis.InputDiagnosticService;
import org.graylog2.inputs.encryption.EncryptedInputConfigs;
import org.graylog2.plugin.configuration.ConfigurationException;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.streams.StreamRule;
import org.graylog2.rest.models.SortOrder;
import org.graylog2.rest.models.system.inputs.requests.InputCreateRequest;
import org.graylog2.rest.models.system.inputs.responses.InputCreated;
import org.graylog2.rest.models.system.inputs.responses.InputDiagnostics;
import org.graylog2.rest.models.system.inputs.responses.InputSummary;
import org.graylog2.rest.models.system.inputs.responses.InputsList;
import org.graylog2.rest.models.tools.responses.PageListResponse;
import org.graylog2.rest.resources.entities.EntityAttribute;
import org.graylog2.rest.resources.entities.EntityDefaults;
import org.graylog2.rest.resources.entities.FilterOption;
import org.graylog2.rest.resources.entities.Sorting;
import org.graylog2.search.SearchQueryField;
import org.graylog2.shared.inputs.MessageInputFactory;
import org.graylog2.shared.inputs.NoSuchInputTypeException;
import org.graylog2.shared.rest.PublicCloudAPI;
import org.graylog2.shared.security.RestPermissions;
import org.graylog2.streams.StreamRuleService;
import org.graylog2.streams.StreamService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@RequiresAuthentication
@PublicCloudAPI
@Tag(name = "System/Inputs", description = "Message inputs")
@Path("/system/inputs")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class InputsResource extends AbstractInputsResource {

    private static final Logger LOG = LoggerFactory.getLogger(InputsResource.class);
    private static final List<EntityAttribute> ATTRIBUTES = List.of(
            EntityAttribute.builder().id(MessageInput.FIELD_ID).title("id").type(SearchQueryField.Type.OBJECT_ID).hidden(true).searchable(true).build(),
            EntityAttribute.builder().id(MessageInput.FIELD_TITLE).title("Title").type(SearchQueryField.Type.STRING).searchable(true).build(),
            EntityAttribute.builder().id(MessageInput.FIELD_TYPE).title("Type").type(SearchQueryField.Type.STRING).searchable(true).build(),
            EntityAttribute.builder().id(MessageInput.FIELD_NODE_ID).title("Node").relatedCollection("nodes")
                    .relatedIdentifier("node_id").relatedDisplayFields(List.of("node_id", "hostname"))
                    .relatedDisplayTemplate("{node_id} ({hostname})").type(SearchQueryField.Type.STRING)
                    .filterable(true).build(),
            EntityAttribute.builder().id(MessageInput.FIELD_GLOBAL).title("Global").type(SearchQueryField.Type.BOOLEAN).filterable(true).build(),
            EntityAttribute.builder().id(MessageInput.FIELD_CREATED_AT).title("Created").type(SearchQueryField.Type.DATE).filterable(true).build(),
            EntityAttribute.builder().id(MessageInput.FIELD_DESIRED_STATE).title("State").type(SearchQueryField.Type.STRING).filterable(false).build(),
            EntityAttribute.builder()
                    .id("runtime_status")
                    .hidden(true)
                    .title("State")
                    .type(SearchQueryField.Type.STRING)
                    .filterable(true)
                    .sortable(false)
                    .filterOptions(getRuntimeStatusFilterOptions())
                    .build()
    );
    private static final EntityDefaults DEFAULTS = EntityDefaults.builder()
            .sort(Sorting.create(MessageInput.FIELD_TITLE, Sorting.Direction.ASC))
            .build();

    private static Set<FilterOption> getRuntimeStatusFilterOptions() {
        return InputRuntimeStatusProvider.STATUS_GROUP_TITLES.entrySet().stream()
                .map(e -> FilterOption.create(e.getKey(), e.getValue()))
                .collect(Collectors.toSet());
    }

    private final InputService inputService;
    private final InputDiagnosticService inputDiagnosticService;
    private final DbQueryCreator dbQueryCreator;
    private final StreamService streamService;
    private final StreamRuleService streamRuleService;
    private final PipelineService pipelineService;
    private final MessageInputFactory messageInputFactory;
    private final Configuration config;
    private final MongoDbInputsMetadataService metadataService;
    private final ClusterEventBus clusterEventBus;

    @Inject
    public InputsResource(InputService inputService,
                          InputDiagnosticService inputDiagnosticService,
                          StreamService streamService,
                          StreamRuleService streamRuleService,
                          PipelineService pipelineService,
                          MessageInputFactory messageInputFactory,
                          Configuration config,
                          MongoDbInputsMetadataService metadataService,
                          ClusterEventBus clusterEventBus,
                          ComputedFieldRegistry computedFieldRegistry) {
        super(messageInputFactory.getAvailableInputs());
        this.inputService = inputService;
        this.inputDiagnosticService = inputDiagnosticService;
        this.dbQueryCreator = new DbQueryCreator(MessageInput.FIELD_TITLE, ATTRIBUTES, computedFieldRegistry);
        this.streamService = streamService;
        this.streamRuleService = streamRuleService;
        this.pipelineService = pipelineService;
        this.messageInputFactory = messageInputFactory;
        this.config = config;
        this.metadataService = metadataService;
        this.clusterEventBus = clusterEventBus;
    }

    @GET
    @Timed
    @Operation(summary = "Get information of a single input on this node")
    @Path("/{inputId}")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Returns the input", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "404", description = "No such input.")
    })
    public InputSummary get(@Parameter(name = "inputId", required = true)
                            @PathParam("inputId") String inputId) throws org.graylog2.database.NotFoundException {
        checkPermission(RestPermissions.INPUTS_READ, inputId);

        final Input input = inputService.find(inputId);

        return getInputSummary(input);
    }

    @GET
    @Timed
    @Operation(summary = "Get diagnostic information of a single input")
    @Path("/diagnostics/{inputId}")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Returns diagnostics", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "404", description = "No such input.")
    })
    public InputDiagnostics diagnostics(@Parameter(name = "inputId", required = true)
                                        @PathParam("inputId") String inputId,
                                        @Context SearchUser searchUser) throws org.graylog2.database.NotFoundException {
        checkPermission(RestPermissions.INPUTS_READ, inputId);
        final Input input = inputService.find(inputId);
        return inputDiagnosticService.getInputDiagnostics(input, searchUser);
    }

    @GET
    @Timed
    @Operation(summary = "Get information about usage of input in pipeline rules")
    @Path("meta/{inputId}")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Returns metadata", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "404", description = "No such input.")
    })
    public PipelineInputsMetadataDao pipelineMetadata(@Parameter(name = "inputId", required = true)
                                                      @PathParam("inputId") String inputId) throws org.graylog2.database.NotFoundException {
        checkPermission(RestPermissions.INPUTS_READ, inputId);
        return filterPipelines(metadataService.getByInputId(inputId));
    }

    @POST
    @Operation(summary = "Bulk retrieval of input metadata")
    @NoAuditEvent("Test resource - doesn't change any data")
    @Path("meta/retrieve")
    public List<PipelineInputsMetadataDao> pipelineMetadataBulk(
            @NotNull @RequestBody(required = true) @NotNull List<String> inputIds) {
        final ImmutableList<PipelineInputsMetadataDao> daoList = metadataService.getByInputIds(
                inputIds.stream()
                        .filter(inputId -> isPermitted(RestPermissions.INPUTS_READ, inputId))
                        .toList());
        return daoList.stream().map(this::filterPipelines).toList();
    }

    private PipelineInputsMetadataDao filterPipelines(PipelineInputsMetadataDao dao) {
        final PipelineInputsMetadataDao.Builder builder = PipelineInputsMetadataDao.builder()
                .id(dao.id())
                .inputId(dao.inputId());
        List<PipelineInputsMetadataDao.MentionedInEntry> mentionedIn = new ArrayList<>();
        for (PipelineInputsMetadataDao.MentionedInEntry entry : dao.mentionedIn()) {
            if (isPermitted(PipelineRestPermissions.PIPELINE_READ, entry.pipelineId())) {
                mentionedIn.add(entry);
            }
        }
        builder.mentionedIn(mentionedIn);
        return builder.build();
    }

    public record InputReferences(
            @JsonProperty("input_id") String inputId,
            @JsonProperty("stream_refs") List<InputReference> streamRefs,
            @JsonProperty("pipeline_refs") List<InputReference> pipelineRefs) {
    }

    public record InputReference(
            @JsonProperty("id") String id,
            @Nullable @JsonProperty("name") String name) {
    }

    @GET
    @Timed
    @Operation(summary = "Returns any streams or pipeline that reference the given input")
    @Path("/references/{inputId}")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Returns references", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "404", description = "No such input.")
    })
    public InputReferences getReferences(@Parameter(name = "inputId", required = true)
                                             @PathParam("inputId") String inputId) {
        checkPermission(RestPermissions.INPUTS_READ, inputId);
        checkPermission(PipelineRestPermissions.PIPELINE_READ);

        return new InputReferences(inputId,
                streamRuleService.loadForInput(inputId).stream()
                        .map(StreamRule::getStreamId)
                        .peek(streamId -> checkPermission(RestPermissions.STREAMS_READ, streamId))
                        .distinct()
                        .map(streamId -> new InputReference(streamId, streamService.streamTitleFromCache(streamId)))
                        .toList(),
                pipelineService.loadBySourcePattern(inputId).stream()
                        .map(pipelineDao -> new InputReference(pipelineDao.id(), pipelineDao.title()))
                        .toList());
    }

    @GET
    @Timed
    @Operation(summary = "Get all inputs")
    public InputsList list() {
        final Set<InputSummary> inputs = inputService.all().stream()
                .filter(input -> isPermitted(RestPermissions.INPUTS_READ, input.getId()))
                .map(this::getInputSummary)
                .collect(Collectors.toSet());

        return InputsList.create(inputs);
    }

    @GET
    @Timed
    @Path("/paginated")
    @Operation(summary = "Get a paginated list of inputs")
    @Produces(MediaType.APPLICATION_JSON)
    public PageListResponse<InputSummary> getPage(@Parameter(name = "page") @QueryParam("page") @DefaultValue("1") int page,
                                                  @Parameter(name = "per_page") @QueryParam("per_page") @DefaultValue("50") int perPage,
                                                  @Parameter(name = "query") @QueryParam("query") @DefaultValue("") String query,
                                                  @Parameter(name = "filters") @QueryParam("filters") List<String> filters,
                                                  @Parameter(name = "sort",
                                                            description = "The field to sort the result on",
                                                            required = true,
                                                            schema = @Schema(allowableValues = {"title", "created_at", "status"}))
                                                  @DefaultValue(MessageInput.FIELD_TITLE) @QueryParam("sort") String sortField,
                                                  @Parameter(name = "order", description = "The sort direction",
                                                            schema = @Schema(allowableValues = {"asc", "desc"}))
                                                      @DefaultValue("asc") @QueryParam("order") SortOrder order,
                                                  @Context HttpHeaders httpHeaders) {
        final Predicate<InputImpl> permissionFilter = input -> isPermitted(RestPermissions.INPUTS_READ, input.getId());

        // Extract authentication token for cluster-wide computed field queries
        final String authToken = org.graylog2.shared.rest.resources.ProxiedResource.authenticationToken(httpHeaders);

        final PaginatedList<Input> result = inputService.paginated(
                dbQueryCreator.createDbQuery(filters, query, authToken),
                permissionFilter,
                order,
                sortField,
                page,
                perPage
        );

        List<InputSummary> summaries = result.stream()
                .map(this::getInputSummary)
                .toList();

        PaginatedList<InputSummary> mappedResult = new PaginatedList<>(
                summaries,
                result.pagination().total(),
                result.pagination().page(),
                result.pagination().perPage(),
                result.grandTotal().orElse(0L)
        );

        return PageListResponse.create(query, mappedResult.pagination(), mappedResult.pagination().total(), sortField, order, mappedResult, ATTRIBUTES, DEFAULTS);
    }

    @POST
    @Timed
    @Operation(
            summary = "Launch input on this node")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Input launched successfully",
                    content = @Content(schema = @Schema(implementation = InputCreated.class))),
            @ApiResponse(responseCode = "404", description = "No such input type registered"),
            @ApiResponse(responseCode = "400", description = "Missing or invalid configuration"),
            @ApiResponse(responseCode = "400", description = "Type is exclusive and already has input running")
    })
    @RequiresPermissions(RestPermissions.INPUTS_CREATE)
    @AuditEvent(type = AuditEventTypes.MESSAGE_INPUT_CREATE)
    public Response create(@Parameter @QueryParam("setup_wizard") @DefaultValue("false") boolean isSetupWizard,
                           @RequestBody(required = true)
                           @Valid @NotNull InputCreateRequest lr) throws ValidationException {
        try {
            throwBadRequestIfNotGlobal(lr);
            // TODO Configuration type values need to be checked. See ConfigurationMapConverter.convertValues()
            final MessageInput messageInput = messageInputFactory.create(lr, getCurrentUser().getName(), lr.node(), isSetupWizard);
            checkPermission(RestPermissions.INPUT_TYPES_CREATE, messageInput.getType());
            if (config.isCloud() && !messageInput.isCloudCompatible()) {
                throw new BadRequestException(String.format(Locale.ENGLISH,
                        "The input type <%s> is not allowed in the cloud environment!", lr.type()));
            }

            messageInput.checkConfiguration();
            final Input input = this.inputService.create(messageInput.asMap());
            final String newId = inputService.save(input);
            final URI inputUri = getUriBuilderToSelf().path(InputsResource.class)
                    .path("{inputId}")
                    .build(newId);

            return Response.created(inputUri).entity(InputCreated.create(newId)).build();
        } catch (NoSuchInputTypeException e) {
            LOG.error("There is no such input type registered.", e);
            throw new NotFoundException("There is no such input type registered.", e);
        } catch (ConfigurationException e) {
            LOG.error("Missing or invalid input configuration.", e);
            throw new BadRequestException("Missing or invalid input configuration.", e);
        }

    }

    @DELETE
    @Timed
    @Path("/{inputId}")
    @Operation(summary = "Terminate input on this node")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Success"),
            @ApiResponse(responseCode = "404", description = "No such input on this node.")
    })
    @AuditEvent(type = AuditEventTypes.MESSAGE_INPUT_DELETE)
    public void terminate(@Parameter(name = "inputId", required = true) @PathParam("inputId") String inputId) throws org.graylog2.database.NotFoundException {
        checkPermission(RestPermissions.INPUTS_TERMINATE, inputId);
        final Input input = inputService.find(inputId);
        checkPermission(RestPermissions.INPUT_TYPES_CREATE, input.getType()); // remove after sharing inputs implemented
        if (0 < inputService.destroy(input)) {
            clusterEventBus.post(new InputDeletedEvent(input.getId(), input.getTitle()));
        }
    }

    @PUT
    @Timed
    @Path("/{inputId}")
    @Operation(
            summary = "Update input on this node")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Input updated successfully",
                    content = @Content(schema = @Schema(implementation = InputCreated.class))),
            @ApiResponse(responseCode = "404", description = "No such input on this node."),
            @ApiResponse(responseCode = "400", description = "Missing or invalid input configuration.")
    })
    @AuditEvent(type = AuditEventTypes.MESSAGE_INPUT_UPDATE)
    public Response update(@RequestBody(required = true) @Valid @NotNull InputCreateRequest lr,
                           @Parameter(name = "inputId", required = true) @PathParam("inputId") String inputId) throws org.graylog2.database.NotFoundException, NoSuchInputTypeException, ConfigurationException, ValidationException {

        throwBadRequestIfNotGlobal(lr);
        checkPermission(RestPermissions.INPUTS_EDIT, inputId);

        final Input input = inputService.find(inputId);
        checkPermission(RestPermissions.INPUT_TYPES_CREATE, input.getType());  // remove after sharing inputs implemented
        final MessageInput messageInput = messageInputFactory.create(lr, getCurrentUser().getName(), lr.node(), input.getDesiredState());

        messageInput.checkConfiguration();

        final Map<String, Object> mergedInput = new HashMap<>(input.getFields());
        mergedInput.putAll(messageInput.asMap());

        // Special handling for encrypted values
        final Map<String, Object> origConfig = input.getConfiguration();
        final Map<String, Object> updatedConfig = Objects.requireNonNullElse(messageInput.getConfiguration().getSource(), Map.of());
        mergedInput.put(MessageInput.FIELD_CONFIGURATION, EncryptedInputConfigs.merge(origConfig, updatedConfig));

        final Input newInput = inputService.create(input.getId(), mergedInput);
        inputService.update(newInput);
        if (!input.getTitle().equals(newInput.getTitle())) {
            clusterEventBus.post(new InputRenamedEvent(input.getId(), input.getTitle(), newInput.getTitle()));
        }

        final URI inputUri = getUriBuilderToSelf().path(InputsResource.class)
                .path("{inputId}")
                .build(input.getId());

        return Response.created(inputUri).entity(InputCreated.create(input.getId())).build();
    }

    private void throwBadRequestIfNotGlobal(InputCreateRequest lr) {
        if ((config.isCloud() || config.isGlobalInputsOnly()) && !lr.global()) {
            throw new BadRequestException("Only global inputs are allowed!");
        }
    }
}

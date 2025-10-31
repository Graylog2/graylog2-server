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
package org.graylog2.contentpacks;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.graph.Graph;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.ImmutableGraph;
import com.google.common.graph.MutableGraph;
import com.google.common.graph.Traverser;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.ForbiddenException;
import org.graylog.grn.GRNRegistry;
import org.graylog.security.GrantDTO;
import org.graylog.security.UserContext;
import org.graylog.security.UserContextMissingException;
import org.graylog.security.shares.EntityShareRequest;
import org.graylog.security.shares.EntitySharesService;
import org.graylog2.Configuration;
import org.graylog2.contentpacks.constraints.ConstraintChecker;
import org.graylog2.contentpacks.exceptions.ContentPackException;
import org.graylog2.contentpacks.exceptions.EmptyDefaultValueException;
import org.graylog2.contentpacks.exceptions.FailedConstraintsException;
import org.graylog2.contentpacks.exceptions.InvalidParameterTypeException;
import org.graylog2.contentpacks.exceptions.InvalidParametersException;
import org.graylog2.contentpacks.exceptions.MissingParametersException;
import org.graylog2.contentpacks.exceptions.UnexpectedEntitiesException;
import org.graylog2.contentpacks.facades.EntityWithExcerptFacade;
import org.graylog2.contentpacks.facades.StreamFacade;
import org.graylog2.contentpacks.facades.UnsupportedEntityFacade;
import org.graylog2.contentpacks.model.ContentPack;
import org.graylog2.contentpacks.model.ContentPackInstallation;
import org.graylog2.contentpacks.model.ContentPackUninstallDetails;
import org.graylog2.contentpacks.model.ContentPackUninstallation;
import org.graylog2.contentpacks.model.ContentPackV1;
import org.graylog2.contentpacks.model.EntityPermissions;
import org.graylog2.contentpacks.model.LegacyContentPack;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.ModelType;
import org.graylog2.contentpacks.model.ModelTypes;
import org.graylog2.contentpacks.model.constraints.Constraint;
import org.graylog2.contentpacks.model.constraints.ConstraintCheckResult;
import org.graylog2.contentpacks.model.entities.Entity;
import org.graylog2.contentpacks.model.entities.EntityDescriptor;
import org.graylog2.contentpacks.model.entities.EntityV1;
import org.graylog2.contentpacks.model.entities.InputEntity;
import org.graylog2.contentpacks.model.entities.NativeEntity;
import org.graylog2.contentpacks.model.entities.NativeEntityDescriptor;
import org.graylog2.contentpacks.model.entities.references.ValueReference;
import org.graylog2.contentpacks.model.entities.references.ValueType;
import org.graylog2.contentpacks.model.parameters.Parameter;
import org.graylog2.plugin.inputs.CloudCompatible;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.shared.users.UserService;
import org.graylog2.streams.StreamService;
import org.graylog2.utilities.Graphs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.graylog2.contentpacks.model.ModelTypes.INPUT_V1;

@Singleton
public class ContentPackService {
    private static final Logger LOG = LoggerFactory.getLogger(ContentPackService.class);

    private final ContentPackInstallationPersistenceService contentPackInstallationPersistenceService;
    private final Set<ConstraintChecker> constraintCheckers;
    private final Map<ModelType, EntityWithExcerptFacade<?, ?>> entityFacades;
    private final ObjectMapper objectMapper;
    private final Configuration configuration;
    private final UserService userService;
    private final StreamService streamService;
    private final GRNRegistry grnRegistry;
    private final EntitySharesService entitySharesService;

    @Inject
    public ContentPackService(ContentPackInstallationPersistenceService contentPackInstallationPersistenceService,
                              Set<ConstraintChecker> constraintCheckers,
                              Map<ModelType, EntityWithExcerptFacade<?, ?>> entityFacades,
                              ObjectMapper objectMapper,
                              Configuration configuration,
                              UserService userService,
                              StreamService streamService,
                              GRNRegistry grnRegistry,
                              EntitySharesService entitySharesService) {
        this.contentPackInstallationPersistenceService = contentPackInstallationPersistenceService;
        this.constraintCheckers = constraintCheckers;
        this.entityFacades = entityFacades;
        this.objectMapper = objectMapper;
        this.configuration = configuration;
        this.userService = userService;
        this.streamService = streamService;
        this.grnRegistry = grnRegistry;
        this.entitySharesService = entitySharesService;
    }

    public ContentPackInstallation installContentPack(ContentPack contentPack,
                                                      Map<String, ValueReference> parameters,
                                                      String comment,
                                                      String username,
                                                      EntityShareRequest shareRequest) {
        return UserContext.runAs(username, userService, () -> {
            try {
                final var userContext = new UserContext.Factory(userService).create();
                return installContentPack(contentPack, parameters, comment, userContext, shareRequest);
            } catch (UserContextMissingException e) {
                throw new IllegalArgumentException("User Context missing", e);
            }
        });
    }

    public ContentPackInstallation installContentPack(ContentPack contentPack,
                                                      Map<String, ValueReference> parameters,
                                                      String comment,
                                                      UserContext userContext,
                                                      EntityShareRequest shareRequest) {
        if (contentPack instanceof ContentPackV1 contentPackV1) {
            return installContentPack(contentPackV1, parameters, comment, userContext, shareRequest);
        } else {
            throw new IllegalArgumentException("Unsupported content pack version: " + contentPack.version());
        }
    }

    private ContentPackInstallation installContentPack(ContentPackV1 contentPack,
                                                       Map<String, ValueReference> parameters,
                                                       String comment,
                                                       UserContext userContext,
                                                       EntityShareRequest shareRequest) {
        ensureConstraints(contentPack.constraints());

        final Entity rootEntity = EntityV1.createRoot(contentPack);
        final ImmutableMap<String, ValueReference> validatedParameters = validateParameters(parameters, contentPack.parameters());
        final ImmutableGraph<Entity> dependencyGraph = buildEntityGraph(rootEntity, contentPack.entities(), validatedParameters);

        final Traverser<Entity> entityTraverser = Traverser.forGraph(dependencyGraph);
        final Iterable<Entity> entitiesInOrder = entityTraverser.depthFirstPostOrder(rootEntity);

        // Insertion order is important for created entities so we can roll back in order!
        final Map<EntityDescriptor, Object> createdEntities = new LinkedHashMap<>();
        final Map<EntityDescriptor, Object> allEntities = getMapWithSystemStreamEntities();
        final ImmutableSet.Builder<NativeEntityDescriptor> allEntityDescriptors = ImmutableSet.builder();

        try {
            for (Entity entity : entitiesInOrder) {
                if (entity.equals(rootEntity)) {
                    continue;
                }

                final EntityDescriptor entityDescriptor = entity.toEntityDescriptor();
                final EntityWithExcerptFacade<?, ?> facade = entityFacades.getOrDefault(entity.type(), UnsupportedEntityFacade.INSTANCE);

                facade.getCreatePermissions(entity).ifPresent(p -> checkPermissions(p, userContext));

                if (configuration.isCloud() && entity.type().equals(INPUT_V1) && entity instanceof EntityV1 entityV1) {
                    final InputEntity inputEntity = objectMapper.convertValue(entityV1.data(), InputEntity.class);
                    String className = inputEntity.type().asString();
                    Class inputClass = Class.forName(className);
                    if (inputClass.getAnnotation(CloudCompatible.class) == null) {
                        LOG.warn("Ignoring incompatible input {} in cloud", className);
                        continue;
                    }
                }

                final Optional<? extends NativeEntity<?>> existingEntity = facade.findExisting(entity, parameters);
                if (existingEntity.isPresent()) {
                    LOG.trace("Found existing entity for {}", entityDescriptor);
                    final NativeEntity<?> nativeEntity = existingEntity.get();
                    final NativeEntityDescriptor nativeEntityDescriptor = nativeEntity.descriptor();
                    /* Found entity on the system or we found a other installation which stated that */
                    if (contentPackInstallationPersistenceService.countInstallationOfEntityById(nativeEntityDescriptor.id()) <= 0 ||
                            contentPackInstallationPersistenceService.countInstallationOfEntityByIdAndFoundOnSystem(nativeEntityDescriptor.id()) > 0) {
                        final NativeEntityDescriptor serverDescriptor = nativeEntityDescriptor.toBuilder()
                                .foundOnSystem(true)
                                .build();
                        allEntityDescriptors.add(serverDescriptor);
                    } else {
                        allEntityDescriptors.add(nativeEntity.descriptor());
                    }
                    allEntities.put(entityDescriptor, nativeEntity.entity());
                } else {
                    LOG.trace("Creating new entity for {}", entityDescriptor);
                    final NativeEntity<?> createdEntity = facade.createNativeEntity(entity, validatedParameters, allEntities, userContext.getUser().getName());
                    allEntityDescriptors.add(createdEntity.descriptor());
                    createdEntities.put(entityDescriptor, createdEntity.entity());
                    allEntities.put(entityDescriptor, createdEntity.entity());
                }
            }
        } catch (Exception e) {
            rollback(createdEntities);

            throw new ContentPackException("Failed to install content pack <" + contentPack.id() + "/" + contentPack.revision() + ">", e);
        }

        final ContentPackInstallation installation = ContentPackInstallation.builder()
                .contentPackId(contentPack.id())
                .contentPackRevision(contentPack.revision())
                .parameters(validatedParameters)
                .comment(comment)
                // TODO: Store complete entity instead of only the descriptor?
                .entities(allEntityDescriptors.build())
                .createdAt(Instant.now())
                .createdBy(userContext.getUser().getName())
                .build();

        shareEntities(installation, shareRequest, userContext);

        return contentPackInstallationPersistenceService.insert(installation);
    }

    public void shareEntities(ContentPackInstallation installation, EntityShareRequest shareRequest, UserContext userContext) {
        if (shareRequest.isEmpty()) {
            return;
        }
        final var user = userContext.getUser();
        final var allEntities = installation.entities();
        final var entityGRNs = allEntities.stream()
                .filter(entity -> grnRegistry.supportsType(entity.type().name()))
                .map(entity -> grnRegistry.newGRN(entity.type().name(), entity.id().id()))
                .toList();
        entityGRNs.forEach((grn) -> entitySharesService.updateEntityShares(grn, shareRequest, user));
    }

    private Map<EntityDescriptor, Object> getMapWithSystemStreamEntities() {
        final Set<String> systemStreamIds = streamService.getSystemStreamIds(true);
        Map<EntityDescriptor, Object> entities = new HashMap<>();
        for (String id : systemStreamIds) {
            try {
                final EntityDescriptor streamEntityDescriptor = EntityDescriptor.create(id, ModelTypes.STREAM_V1);
                final StreamFacade streamFacade = (StreamFacade) entityFacades.getOrDefault(ModelTypes.STREAM_V1, UnsupportedEntityFacade.INSTANCE);
                final Entity streamEntity = streamFacade.exportEntity(streamEntityDescriptor, EntityDescriptorIds.withSystemStreams(systemStreamIds, streamEntityDescriptor)).get();
                final NativeEntity<Stream> streamNativeEntity = streamFacade.findExisting(streamEntity, Collections.emptyMap()).get();
                entities.put(streamEntityDescriptor, streamNativeEntity.entity());
            } catch (Exception e) {
                LOG.debug("Failed to load system stream <{}>", id, e);
            }
        }
        return entities;
    }

    @SuppressWarnings("unchecked")
    private void rollback(Map<EntityDescriptor, Object> entities) {
        final ImmutableList<Map.Entry<EntityDescriptor, Object>> entries = ImmutableList.copyOf(entities.entrySet());
        for (Map.Entry<EntityDescriptor, Object> entry : entries.reverse()) {
            final EntityDescriptor entityDescriptor = entry.getKey();
            final Object entity = entry.getValue();
            final EntityWithExcerptFacade facade = entityFacades.getOrDefault(entityDescriptor.type(), UnsupportedEntityFacade.INSTANCE);

            LOG.debug("Removing entity {}", entityDescriptor);
            facade.delete(entity);
        }
    }

    public ContentPackUninstallDetails getUninstallDetails(ContentPack contentPack, ContentPackInstallation installation) {
        if (contentPack instanceof ContentPackV1 contentPackV1) {
            return getUninstallDetails(contentPackV1, installation);
        } else {
            throw new IllegalArgumentException("Unsupported content pack version: " + contentPack.version());
        }
    }

    private ContentPackUninstallDetails getUninstallDetails(ContentPackV1 contentPack, ContentPackInstallation installation) {
        final Entity rootEntity = EntityV1.createRoot(contentPack);
        final ImmutableMap<String, ValueReference> parameters = installation.parameters();
        final ImmutableGraph<Entity> dependencyGraph = buildEntityGraph(rootEntity, contentPack.entities(), parameters);

        final Traverser<Entity> entityTraverser = Traverser.forGraph(dependencyGraph);
        final Iterable<Entity> entitiesInOrder = entityTraverser.breadthFirst(rootEntity);
        final Set<NativeEntityDescriptor> nativeEntityDescriptors = new HashSet<>();

        entitiesInOrder.forEach((entity -> {
            if (entity.equals(rootEntity)) {
                return;
            }

            final Optional<NativeEntityDescriptor> nativeEntityDescriptorOptional = installation.entities().stream()
                    .filter(descriptor -> entity.id().equals(descriptor.contentPackEntityId()))
                    .findFirst();
            if (nativeEntityDescriptorOptional.isPresent()) {
                NativeEntityDescriptor nativeEntityDescriptor = nativeEntityDescriptorOptional.get();
                if (contentPackInstallationPersistenceService
                        .countInstallationOfEntityById(nativeEntityDescriptor.id()) <= 1) {
                    nativeEntityDescriptors.add(nativeEntityDescriptor);
                }
            }
        }));

        return ContentPackUninstallDetails.create(nativeEntityDescriptors);
    }

    public ContentPackUninstallation uninstallContentPack(ContentPack contentPack, ContentPackInstallation installation) {
        /*
         * - Show entities marked for removal and ask user for confirmation
         * - Resolve dependency order of the previously created entities
         * - Stop/pause entities in reverse order
         *      - In case of error: Ignore, log error message (or create system notification), and continue
         * - Remove entities in reverse order
         *      - In case of error: Ignore, log error message (or create system notification), and continue
         * - Remove content pack snapshot
         */
        if (contentPack instanceof ContentPackV1) {
            return uninstallContentPack(installation, (ContentPackV1) contentPack);
        } else {
            throw new IllegalArgumentException("Unsupported content pack version: " + contentPack.version());
        }
    }

    private ContentPackUninstallation uninstallContentPack(ContentPackInstallation installation, ContentPackV1 contentPack) {
        final Entity rootEntity = EntityV1.createRoot(contentPack);
        final ImmutableMap<String, ValueReference> parameters = installation.parameters();
        final ImmutableGraph<Entity> dependencyGraph = buildEntityGraph(rootEntity, contentPack.entities(), parameters);

        final Traverser<Entity> entityTraverser = Traverser.forGraph(dependencyGraph);
        final Iterable<Entity> entitiesInOrder = entityTraverser.breadthFirst(rootEntity);

        final Set<NativeEntityDescriptor> removedEntities = new HashSet<>();
        final Map<ModelId, Object> removedEntityObjects = new HashMap<>();
        final Set<NativeEntityDescriptor> failedEntities = new HashSet<>();
        final Set<NativeEntityDescriptor> skippedEntities = new HashSet<>();
        final Map<ModelId, List<GrantDTO>> entityGrants = new HashMap<>();

        try {
            for (Entity entity : entitiesInOrder) {
                if (entity.equals(rootEntity)) {
                    continue;
                }

                final Optional<NativeEntityDescriptor> nativeEntityDescriptorOptional = installation.entities().stream()
                        .filter(descriptor -> entity.id().equals(descriptor.contentPackEntityId()))
                        .findFirst();

                final EntityWithExcerptFacade facade = entityFacades.getOrDefault(entity.type(), UnsupportedEntityFacade.INSTANCE);

                if (nativeEntityDescriptorOptional.isPresent()) {
                    final NativeEntityDescriptor nativeEntityDescriptor = nativeEntityDescriptorOptional.get();
                    final Optional nativeEntityOptional = facade.loadNativeEntity(nativeEntityDescriptor);
                    final ModelId entityId = nativeEntityDescriptor.id();
                    final long installCount = contentPackInstallationPersistenceService
                            .countInstallationOfEntityById(entityId);
                    final long systemFoundCount = contentPackInstallationPersistenceService.
                            countInstallationOfEntityByIdAndFoundOnSystem(entityId);

                    if (installCount > 1 || (installCount == 1 && systemFoundCount >= 1)) {
                        skippedEntities.add(nativeEntityDescriptor);
                        LOG.debug("Did not remove entity since other content pack installations still use them: {}",
                                nativeEntityDescriptor);
                    } else if (nativeEntityOptional.isPresent()) {
                        final Object nativeEntity = nativeEntityOptional.get();
                        LOG.trace("Removing existing native entity for {}", nativeEntityDescriptor);
                        try {
                            //noinspection unchecked
                            List<GrantDTO> grants = facade.resolveGrants(((NativeEntity) nativeEntity).entity());
                            entityGrants.put(entity.id(), grants);
                            // The EntityFacade#delete() method expects the actual entity object
                            //noinspection unchecked
                            facade.delete(((NativeEntity) nativeEntity).entity());
                            removedEntities.add(nativeEntityDescriptor);
                            removedEntityObjects.put(nativeEntityDescriptor.contentPackEntityId(), ((NativeEntity) nativeEntity).entity());
                        } catch (Exception e) {
                            LOG.warn("Couldn't remove native entity {}", nativeEntity);
                            failedEntities.add(nativeEntityDescriptor);
                        }
                    } else {
                        LOG.trace("Couldn't find existing native entity for {}", nativeEntityDescriptor);
                    }
                }
            }
        } catch (Exception e) {
            throw new ContentPackException("Failed to remove content pack <" + contentPack.id() + "/" + contentPack.revision() + ">", e);
        }

        final int deletedInstallations = contentPackInstallationPersistenceService.deleteById(installation.id());
        LOG.debug("Deleted {} installation(s) of content pack {}", deletedInstallations, contentPack.id());

        return ContentPackUninstallation.builder()
                .entities(ImmutableSet.copyOf(removedEntities))
                .entityObjects(ImmutableMap.copyOf(removedEntityObjects))
                .skippedEntities(ImmutableSet.copyOf(skippedEntities))
                .failedEntities(ImmutableSet.copyOf(failedEntities))
                .entityGrants(ImmutableMap.copyOf(entityGrants))
                .build();
    }

    private ImmutableGraph<Entity> buildEntityGraph(Entity rootEntity,
                                                    Set<Entity> entities,
                                                    Map<String, ValueReference> parameters) {
        final Map<EntityDescriptor, Entity> entityDescriptorMap = entities.stream()
                .collect(Collectors.toMap(Entity::toEntityDescriptor, Function.identity()));

        final MutableGraph<Entity> dependencyGraph = GraphBuilder.directed()
                .allowsSelfLoops(false)
                .expectedNodeCount(entities.size())
                .build();

        for (Map.Entry<EntityDescriptor, Entity> entry : entityDescriptorMap.entrySet()) {
            final EntityDescriptor entityDescriptor = entry.getKey();
            final Entity entity = entry.getValue();

            final EntityWithExcerptFacade<?, ?> facade = entityFacades.getOrDefault(entity.type(), UnsupportedEntityFacade.INSTANCE);
            final Graph<Entity> entityGraph = facade.resolveForInstallation(entity, parameters, entityDescriptorMap);
            LOG.trace("Dependencies of entity {}: {}", entityDescriptor, entityGraph);

            dependencyGraph.putEdge(rootEntity, entity);
            Graphs.merge(dependencyGraph, entityGraph);
            LOG.trace("New dependency graph: {}", dependencyGraph);
        }

        final Set<Entity> unexpectedEntities = dependencyGraph.nodes().stream()
                .filter(entity -> !rootEntity.equals(entity))
                .filter(entity -> !entities.contains(entity))
                .collect(Collectors.toSet());

        if (!unexpectedEntities.isEmpty()) {
            throw new UnexpectedEntitiesException(unexpectedEntities);
        }

        return ImmutableGraph.copyOf(dependencyGraph);
    }

    private void ensureConstraints(Set<Constraint> requiredConstraints) {
        final Set<Constraint> fulfilledConstraints = new HashSet<>();
        for (ConstraintChecker constraintChecker : constraintCheckers) {
            fulfilledConstraints.addAll(constraintChecker.ensureConstraints(requiredConstraints));
        }

        if (!fulfilledConstraints.equals(requiredConstraints)) {
            final Set<Constraint> failedConstraints = Sets.difference(requiredConstraints, fulfilledConstraints);
            throw new FailedConstraintsException(failedConstraints);
        }
    }

    public Set<ConstraintCheckResult> checkConstraints(ContentPack contentPack) {
        if (contentPack instanceof ContentPackV1 contentPackV1) {
            return checkConstraintsV1(contentPackV1);
        } else if (contentPack instanceof LegacyContentPack) {
            return Collections.emptySet();
        } else {
            throw new IllegalArgumentException("Unsupported content pack version: " + contentPack.version());
        }
    }

    private Set<ConstraintCheckResult> checkConstraintsV1(ContentPackV1 contentPackV1) {
        Set<Constraint> requiredConstraints = contentPackV1.constraints();
        final Set<ConstraintCheckResult> fulfilledConstraints = new HashSet<>();
        for (ConstraintChecker constraintChecker : constraintCheckers) {
            fulfilledConstraints.addAll(constraintChecker.checkConstraints(requiredConstraints));
        }
        return fulfilledConstraints;
    }

    private ImmutableMap<String, ValueReference> validateParameters(Map<String, ValueReference> parameters,
                                                                    Set<Parameter> contentPackParameters) {
        final Set<String> contentPackParameterNames = contentPackParameters.stream()
                .map(Parameter::name)
                .collect(Collectors.toSet());

        checkUnknownParameters(parameters, contentPackParameterNames);
        checkMissingParameters(parameters, contentPackParameterNames);

        final Map<String, ValueType> contentPackParameterValueTypes = contentPackParameters.stream()
                .collect(Collectors.toMap(Parameter::name, Parameter::valueType));
        final Set<String> invalidParameters = parameters.entrySet().stream()
                .filter(entry -> entry.getValue().valueType() != contentPackParameterValueTypes.get(entry.getKey()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        if (!invalidParameters.isEmpty()) {
            throw new InvalidParametersException(invalidParameters);
        }

        final ImmutableMap.Builder<String, ValueReference> validatedParameters = ImmutableMap.builder();
        for (Parameter contentPackParameter : contentPackParameters) {
            final String name = contentPackParameter.name();
            final ValueReference providedParameter = parameters.get(name);

            if (providedParameter == null) {
                final Optional<?> defaultValue = contentPackParameter.defaultValue();
                final Object value = defaultValue.orElseThrow(() -> new EmptyDefaultValueException(name));
                final ValueReference valueReference = ValueReference.builder()
                        .valueType(contentPackParameter.valueType())
                        .value(value)
                        .build();
                validatedParameters.put(name, valueReference);
            } else if (providedParameter.valueType() != contentPackParameter.valueType()) {
                throw new InvalidParameterTypeException(contentPackParameter.valueType(), providedParameter.valueType());
            } else {
                validatedParameters.put(name, providedParameter);
            }
        }

        return validatedParameters.build();
    }

    private void checkUnknownParameters(Map<String, ValueReference> parameters, Set<String> contentPackParameterNames) {
        final Predicate<String> containsContentPackParameter = contentPackParameterNames::contains;
        final Set<String> unknownParameters = parameters.keySet().stream()
                .filter(containsContentPackParameter.negate())
                .collect(Collectors.toSet());
        if (!unknownParameters.isEmpty()) {
            // Ignore unknown parameters for now
            LOG.debug("Unknown parameters: {}", unknownParameters);
        }
    }

    private void checkMissingParameters(Map<String, ValueReference> parameters, Set<String> contentPackParameterNames) {
        final Predicate<String> containsParameter = parameters::containsKey;
        final Set<String> missingParameters = contentPackParameterNames.stream()
                .filter(containsParameter.negate())
                .collect(Collectors.toSet());
        if (!missingParameters.isEmpty()) {
            throw new MissingParametersException(missingParameters);
        }
    }

    private void checkPermissions(EntityPermissions permissions, UserContext userContext) {
        if (!permissions.isPermitted(userContext)) {
            permissions.permissions().stream()
                    .filter(p -> !userContext.isPermitted(p))
                    .forEach(p -> LOG.error("Missing permission <{}> (Logical {})", p, permissions.operator()));
            throw new ForbiddenException("Missing permissions");
        }
    }
}

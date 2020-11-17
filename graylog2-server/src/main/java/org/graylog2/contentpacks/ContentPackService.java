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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.graph.ElementOrder;
import com.google.common.graph.Graph;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.ImmutableGraph;
import com.google.common.graph.MutableGraph;
import com.google.common.graph.Traverser;
import org.graylog2.contentpacks.constraints.ConstraintChecker;
import org.graylog2.contentpacks.exceptions.ContentPackException;
import org.graylog2.contentpacks.exceptions.EmptyDefaultValueException;
import org.graylog2.contentpacks.exceptions.FailedConstraintsException;
import org.graylog2.contentpacks.exceptions.InvalidParameterTypeException;
import org.graylog2.contentpacks.exceptions.InvalidParametersException;
import org.graylog2.contentpacks.exceptions.MissingParametersException;
import org.graylog2.contentpacks.exceptions.UnexpectedEntitiesException;
import org.graylog2.contentpacks.facades.EntityFacade;
import org.graylog2.contentpacks.facades.UnsupportedEntityFacade;
import org.graylog2.contentpacks.model.ContentPack;
import org.graylog2.contentpacks.model.ContentPackInstallation;
import org.graylog2.contentpacks.model.ContentPackUninstallDetails;
import org.graylog2.contentpacks.model.ContentPackUninstallation;
import org.graylog2.contentpacks.model.ContentPackV1;
import org.graylog2.contentpacks.model.LegacyContentPack;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.ModelType;
import org.graylog2.contentpacks.model.constraints.Constraint;
import org.graylog2.contentpacks.model.constraints.ConstraintCheckResult;
import org.graylog2.contentpacks.model.entities.Entity;
import org.graylog2.contentpacks.model.entities.EntityDescriptor;
import org.graylog2.contentpacks.model.entities.EntityExcerpt;
import org.graylog2.contentpacks.model.entities.EntityV1;
import org.graylog2.contentpacks.model.entities.NativeEntity;
import org.graylog2.contentpacks.model.entities.NativeEntityDescriptor;
import org.graylog2.contentpacks.model.entities.references.ValueReference;
import org.graylog2.contentpacks.model.entities.references.ValueType;
import org.graylog2.contentpacks.model.parameters.Parameter;
import org.graylog2.utilities.Graphs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Singleton
public class ContentPackService {
    private static final Logger LOG = LoggerFactory.getLogger(ContentPackService.class);

    private final ContentPackInstallationPersistenceService contentPackInstallationPersistenceService;
    private final Set<ConstraintChecker> constraintCheckers;
    private final Map<ModelType, EntityFacade<?>> entityFacades;

    @Inject
    public ContentPackService(ContentPackInstallationPersistenceService contentPackInstallationPersistenceService,
                              Set<ConstraintChecker> constraintCheckers,
                              Map<ModelType, EntityFacade<?>> entityFacades) {
        this.contentPackInstallationPersistenceService = contentPackInstallationPersistenceService;
        this.constraintCheckers = constraintCheckers;
        this.entityFacades = entityFacades;
    }

    public ContentPackInstallation installContentPack(ContentPack contentPack,
                                                      Map<String, ValueReference> parameters,
                                                      String comment,
                                                      String user) {
        if (contentPack instanceof ContentPackV1) {
            return installContentPack((ContentPackV1) contentPack, parameters, comment, user);
        } else {
            throw new IllegalArgumentException("Unsupported content pack version: " + contentPack.version());
        }
    }

    private ContentPackInstallation installContentPack(ContentPackV1 contentPack,
                                                      Map<String, ValueReference> parameters,
                                                      String comment,
                                                      String user) {
        ensureConstraints(contentPack.constraints());

        final Entity rootEntity = EntityV1.createRoot(contentPack);
        final ImmutableMap<String, ValueReference> validatedParameters = validateParameters(parameters, contentPack.parameters());
        final ImmutableGraph<Entity> dependencyGraph = buildEntityGraph(rootEntity, contentPack.entities(), validatedParameters);

        final Traverser<Entity> entityTraverser = Traverser.forGraph(dependencyGraph);
        final Iterable<Entity> entitiesInOrder = entityTraverser.depthFirstPostOrder(rootEntity);

        // Insertion order is important for created entities so we can roll back in order!
        final Map<EntityDescriptor, Object> createdEntities = new LinkedHashMap<>();
        final Map<EntityDescriptor, Object> allEntities = new HashMap<>();
        final ImmutableSet.Builder<NativeEntityDescriptor> allEntityDescriptors = ImmutableSet.builder();

        try {
            for (Entity entity : entitiesInOrder) {
                if (entity.equals(rootEntity)) {
                    continue;
                }

                final EntityDescriptor entityDescriptor = entity.toEntityDescriptor();
                final EntityFacade facade = entityFacades.getOrDefault(entity.type(), UnsupportedEntityFacade.INSTANCE);
                @SuppressWarnings({"rawtypes", "unchecked"}) final Optional<NativeEntity> existingEntity = facade.findExisting(entity, parameters);
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
                    final NativeEntity<?> createdEntity = facade.createNativeEntity(entity, validatedParameters, allEntities, user);
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
                .createdBy(user)
                .build();

        return contentPackInstallationPersistenceService.insert(installation);
    }

    @SuppressWarnings("unchecked")
    private void rollback(Map<EntityDescriptor, Object> entities) {
        final ImmutableList<Map.Entry<EntityDescriptor, Object>> entries = ImmutableList.copyOf(entities.entrySet());
        for (Map.Entry<EntityDescriptor, Object> entry : entries.reverse()) {
            final EntityDescriptor entityDescriptor = entry.getKey();
            final Object entity = entry.getValue();
            final EntityFacade facade = entityFacades.getOrDefault(entityDescriptor.type(), UnsupportedEntityFacade.INSTANCE);

            LOG.debug("Removing entity {}", entityDescriptor);
            facade.delete(entity);
        }
    }

    public ContentPackUninstallDetails getUninstallDetails(ContentPack contentPack, ContentPackInstallation installation) {
        if (contentPack instanceof ContentPackV1) {
            return getUninstallDetails((ContentPackV1) contentPack, installation);
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
        final Set<NativeEntityDescriptor> failedEntities = new HashSet<>();
        final Set<NativeEntityDescriptor> skippedEntities = new HashSet<>();

        try {
            for (Entity entity : entitiesInOrder) {
                if (entity.equals(rootEntity)) {
                    continue;
                }

                final Optional<NativeEntityDescriptor> nativeEntityDescriptorOptional = installation.entities().stream()
                        .filter(descriptor -> entity.id().equals(descriptor.contentPackEntityId()))
                        .findFirst();

                final EntityFacade facade = entityFacades.getOrDefault(entity.type(), UnsupportedEntityFacade.INSTANCE);

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
                        LOG.trace("Removing existing native entity for {} ({})", nativeEntityDescriptor);
                        try {
                            // The EntityFacade#delete() method expects the actual entity object
                            //noinspection unchecked
                            facade.delete(((NativeEntity) nativeEntity).entity());
                            removedEntities.add(nativeEntityDescriptor);
                        } catch (Exception e) {
                            LOG.warn("Couldn't remove native entity {}", nativeEntity);
                            failedEntities.add(nativeEntityDescriptor);
                        }
                    } else {
                        LOG.trace("Couldn't find existing native entity for {} ({})", nativeEntityDescriptor);
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
                .skippedEntities(ImmutableSet.copyOf(skippedEntities))
                .failedEntities(ImmutableSet.copyOf(failedEntities))
                .build();
    }

    public Set<EntityExcerpt> listAllEntityExcerpts() {
        final ImmutableSet.Builder<EntityExcerpt> entityIndexBuilder = ImmutableSet.builder();
        entityFacades.values().forEach(facade -> entityIndexBuilder.addAll(facade.listEntityExcerpts()));
        return entityIndexBuilder.build();
    }

    public Set<EntityDescriptor> resolveEntities(Collection<EntityDescriptor> unresolvedEntities) {
        final MutableGraph<EntityDescriptor> dependencyGraph = GraphBuilder.directed()
                .allowsSelfLoops(false)
                .nodeOrder(ElementOrder.insertion())
                .build();
        unresolvedEntities.forEach(dependencyGraph::addNode);

        final HashSet<EntityDescriptor> resolvedEntities = new HashSet<>();
        final MutableGraph<EntityDescriptor> finalDependencyGraph = resolveDependencyGraph(dependencyGraph, resolvedEntities);

        LOG.debug("Final dependency graph: {}", finalDependencyGraph);

        return finalDependencyGraph.nodes();
    }

    private MutableGraph<EntityDescriptor> resolveDependencyGraph(Graph<EntityDescriptor> dependencyGraph, Set<EntityDescriptor> resolvedEntities) {
        final MutableGraph<EntityDescriptor> mutableGraph = GraphBuilder.from(dependencyGraph).build();
        Graphs.merge(mutableGraph, dependencyGraph);

        for (EntityDescriptor entityDescriptor : dependencyGraph.nodes()) {
            LOG.debug("Resolving entity {}", entityDescriptor);
            if (resolvedEntities.contains(entityDescriptor)) {
                LOG.debug("Entity {} already resolved, skipping.", entityDescriptor);
                continue;
            }

            final EntityFacade<?> facade = entityFacades.getOrDefault(entityDescriptor.type(), UnsupportedEntityFacade.INSTANCE);
            final Graph<EntityDescriptor> graph = facade.resolveNativeEntity(entityDescriptor);
            LOG.trace("Dependencies of entity {}: {}", entityDescriptor, graph);

            Graphs.merge(mutableGraph, graph);
            LOG.trace("New dependency graph: {}", mutableGraph);

            resolvedEntities.add(entityDescriptor);
            final Graph<EntityDescriptor> result = resolveDependencyGraph(mutableGraph, resolvedEntities);
            Graphs.merge(mutableGraph, result);
        }

        return mutableGraph;
    }

    public ImmutableSet<Entity> collectEntities(Collection<EntityDescriptor> resolvedEntities) {
        // It's important to only compute the EntityDescriptor IDs once per #collectEntities call! Otherwise we
        // will get broken references between the entities.
        final EntityDescriptorIds entityDescriptorIds = EntityDescriptorIds.of(resolvedEntities);

        final ImmutableSet.Builder<Entity> entities = ImmutableSet.builder();
        for (EntityDescriptor entityDescriptor : resolvedEntities) {
            if (EntityDescriptorIds.isDefaultStreamDescriptor(entityDescriptor)) {
                continue;
            }
            final EntityFacade<?> facade = entityFacades.getOrDefault(entityDescriptor.type(), UnsupportedEntityFacade.INSTANCE);

            facade.exportEntity(entityDescriptor, entityDescriptorIds).ifPresent(entities::add);
        }

        return entities.build();
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

            final EntityFacade<?> facade = entityFacades.getOrDefault(entity.type(), UnsupportedEntityFacade.INSTANCE);
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
        if (contentPack instanceof ContentPackV1) {
            return checkConstraintsV1((ContentPackV1) contentPack);
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
}

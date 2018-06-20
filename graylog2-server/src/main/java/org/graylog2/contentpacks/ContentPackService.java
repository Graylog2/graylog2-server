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
package org.graylog2.contentpacks;

import com.fasterxml.jackson.databind.node.NullNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.graph.Graph;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.ImmutableGraph;
import com.google.common.graph.MutableGraph;
import com.google.common.graph.Traverser;
import org.graylog2.contentpacks.constraints.ConstraintChecker;
import org.graylog2.contentpacks.facades.EntityFacade;
import org.graylog2.contentpacks.facades.UnsupportedEntityFacade;
import org.graylog2.contentpacks.model.ContentPack;
import org.graylog2.contentpacks.model.ContentPackInstallation;
import org.graylog2.contentpacks.model.ContentPackV1;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.ModelType;
import org.graylog2.contentpacks.model.ModelTypes;
import org.graylog2.contentpacks.model.constraints.Constraint;
import org.graylog2.contentpacks.model.entities.Entity;
import org.graylog2.contentpacks.model.entities.EntityDescriptor;
import org.graylog2.contentpacks.model.entities.EntityV1;
import org.graylog2.contentpacks.model.entities.NativeEntity;
import org.graylog2.contentpacks.model.entities.references.ValueReference;
import org.graylog2.contentpacks.model.entities.references.ValueType;
import org.graylog2.contentpacks.model.entities.references.ValueTyped;
import org.graylog2.contentpacks.model.parameters.Parameter;
import org.graylog2.utilities.Graphs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Instant;
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

    private final ContentPackPersistenceService contentPackPersistenceService;
    private final ContentPackInstallationPersistenceService contentPackInstallationPersistenceService;
    private final Set<ConstraintChecker> constraintCheckers;
    private final Map<ModelType, EntityFacade<?>> entityFacades;

    @Inject
    public ContentPackService(ContentPackPersistenceService contentPackPersistenceService,
                              ContentPackInstallationPersistenceService contentPackInstallationPersistenceService,
                              Set<ConstraintChecker> constraintCheckers,
                              Map<ModelType, EntityFacade<?>> entityFacades) {
        this.contentPackPersistenceService = contentPackPersistenceService;
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

    public ContentPackInstallation installContentPack(ContentPackV1 contentPack,
                                                      Map<String, ValueReference> parameters,
                                                      String comment,
                                                      String user) {
        checkConstraints(contentPack.requires());

        final Entity rootEntity = EntityV1.builder()
                .type(ModelTypes.ROOT)
                .id(ModelId.of("virtual-root-" + contentPack.id() + "-" + contentPack.revision()))
                .data(NullNode.getInstance())
                .build();
        final ImmutableMap<String, ValueReference> validatedParameters = validateParameters(parameters, contentPack.parameters());
        final ImmutableGraph<Entity> dependencyGraph = buildEntityGraph(rootEntity, contentPack.entities(), validatedParameters);

        final Traverser<Entity> entityTraverser = Traverser.forGraph(dependencyGraph);
        final Iterable<Entity> entitiesInOrder = entityTraverser.depthFirstPostOrder(rootEntity);

        final Map<EntityDescriptor, Object> createdEntities = new LinkedHashMap<>();
        final Map<EntityDescriptor, Object> allEntities = new HashMap<>();
        final ImmutableSet.Builder<EntityDescriptor> allEntityDescriptors = ImmutableSet.builder();

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
                    allEntityDescriptors.add(nativeEntity.descriptor());
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

            // TODO: Use custom exception
            throw new RuntimeException("Failed to install content pack <" + contentPack.id() + "/" + contentPack.revision() + ">", e);
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

    public ContentPackInstallation uninstallContentPack(ContentPackInstallation installation) {
        /*
         * - Show entities marked for removal and ask user for confirmation
         * - Resolve dependency order of the previously created entities
         * - Stop/pause entities in reverse order
         *      - In case of error: Ignore, log error message (or create system notification), and continue
         * - Remove entities in reverse order
         *      - In case of error: Ignore, log error message (or create system notification), and continue
         * - Remove content pack snapshot
         */

        throw new UnsupportedOperationException();
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
            final Graph<Entity> entityGraph = facade.resolve(entity, parameters, entityDescriptorMap);
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

    private void checkConstraints(Set<Constraint> requiredConstraints) {
        final Set<Constraint> fulfilledConstraints = new HashSet<>();
        for (ConstraintChecker constraintChecker : constraintCheckers) {
            fulfilledConstraints.addAll(constraintChecker.checkConstraints(requiredConstraints));
        }

        if (!fulfilledConstraints.equals(requiredConstraints)) {
            final Set<Constraint> failedConstraints = Sets.difference(requiredConstraints, fulfilledConstraints);
            throw new FailedConstraintsException(failedConstraints);
        }
    }

    private ImmutableMap<String, ValueReference> validateParameters(Map<String, ValueReference> parameters,
                                                                    Set<Parameter> contentPackParameters) {
        final Set<String> contentPackParameterNames = contentPackParameters.stream()
                .map(Parameter::name)
                .collect(Collectors.toSet());

        checkUnknownParameters(parameters, contentPackParameterNames);
        checkMissingParameters(parameters, contentPackParameterNames);

        final Map<String, ValueType> contentPackParameterValueTypes = contentPackParameters.stream()
                .collect(Collectors.toMap(Parameter::name, ValueTyped::valueType));
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

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
package org.graylog2.contentpacks.facades;

import org.graylog2.contentpacks.model.entities.Entity;
import org.graylog2.contentpacks.model.entities.EntityDescriptor;
import org.graylog2.contentpacks.model.entities.NativeEntity;
import org.graylog2.contentpacks.model.entities.references.ValueReference;

import java.util.Map;

/**
 * Capability interface for facades that support updating an existing native entity in place
 * during a content pack upgrade, preserving the entity's ID.
 *
 * <p>During a content pack upgrade ({@link org.graylog2.contentpacks.ContentPackService#upgradeContentPack}),
 * a facade that implements this interface has its existing native entity updated in place, preserving
 * the entity's ID. A facade that does not implement it falls back to delete-and-recreate: the content
 * is still refreshed from the new revision, but the entity gets a new ID.</p>
 *
 * <p>Any entity type shipped in a content pack whose ID must survive upgrades (e.g. Illuminate
 * spotlight-pack types such as event definitions, dashboards, and stream references) must therefore
 * opt in — even if the implementation deliberately leaves the entity untouched (see
 * {@link StreamReferenceFacade}). Types not shipped in upgradable packs, or for which a new ID on
 * upgrade is acceptable, can simply omit it.</p>
 */
public interface UpdatableEntityFacade<EntityClass> {

    /**
     * Update an existing native entity's content from the content pack entity, preserving its ID.
     *
     * @param entity         the content pack entity carrying the new content
     * @param existingEntity the existing native entity to update in place
     * @param parameters     user-provided parameters to resolve parameters in the entity model
     * @param nativeEntities existing native entities to reference during the update
     * @param username       the name of the user performing the upgrade
     */
    void updateNativeEntity(Entity entity,
                            NativeEntity<EntityClass> existingEntity,
                            Map<String, ValueReference> parameters,
                            Map<EntityDescriptor, Object> nativeEntities,
                            String username);
}

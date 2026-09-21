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
import groupBy from 'lodash/groupBy';

import type Entity from 'logic/content-packs/Entity';
import EntityIndex from 'logic/content-packs/EntityIndex';

export const SERVER_SOURCE = 'server';
export const CONTENT_PACK_SOURCE = 'contentPack';

export type EntitySource = typeof SERVER_SOURCE | typeof CONTENT_PACK_SOURCE;

/*
 * When editing a content pack, the same entity can exist twice: once as it is installed on this server right now,
 * and once as it was frozen into the previous revision. Both are offered as the export source for a single entity,
 * so they are merged into one row here instead of being listed separately.
 *
 * The two copies do not share an id (the server uses its own ids, the pack its own), so they can only be paired
 * through the installation record, which stores both. A revision that was never installed has no such record and
 * its entities stay unpaired.
 */
export type CatalogEntity = {
  id: string;
  type: { name: string };
  title: string;
  serverEntity?: EntityIndex;
  packEntity?: Entity;
};

export type EntityCatalog = { [entityType: string]: Array<CatalogEntity> };

type EntityIndexByType = { [entityType: string]: Array<EntityIndex> };

type InstalledEntity = {
  id: string;
  content_pack_entity_id: string;
};

type ContentPackInstallation = {
  content_pack_revision: number;
  entities: Array<InstalledEntity>;
};

/*
 * Maps a pack entity id to the id of the entity that installing it created on this server. Installations of the
 * revision being edited are applied last so they win over older ones.
 */
export const installedEntityIds = (
  installations: Array<ContentPackInstallation> | undefined,
  revision: number | undefined,
): { [packEntityId: string]: string } => {
  const ordered = [...(installations ?? [])].sort(
    (a, b) => Number(a.content_pack_revision === revision) - Number(b.content_pack_revision === revision),
  );

  return Object.fromEntries(
    ordered.flatMap((installation) =>
      (installation.entities ?? []).map((entity) => [entity.content_pack_entity_id, entity.id]),
    ),
  );
};

export const mergeEntityCatalog = (
  entityIndex: EntityIndexByType | undefined,
  contentPackEntities: Array<Entity> | undefined,
  serverIdsByPackEntityId: { [packEntityId: string]: string } = {},
): EntityCatalog => {
  const packEntitiesByType: { [entityType: string]: Array<Entity> } = groupBy(contentPackEntities ?? [], 'type.name');
  const entityTypes = new Set([...Object.keys(entityIndex ?? {}), ...Object.keys(packEntitiesByType)]);

  return Object.fromEntries(
    [...entityTypes].map((entityType) => {
      const rows = new Map<string, CatalogEntity>();

      (entityIndex?.[entityType] ?? []).forEach((serverEntity) => {
        rows.set(serverEntity.id, {
          id: serverEntity.id,
          type: serverEntity.type as unknown as { name: string },
          title: serverEntity.title,
          serverEntity,
        });
      });

      (packEntitiesByType[entityType] ?? []).forEach((packEntity) => {
        const key = serverIdsByPackEntityId[packEntity.id] ?? packEntity.id;
        const row = rows.get(key);

        rows.set(
          key,
          row
            ? { ...row, packEntity }
            : { id: packEntity.id, type: packEntity.type, title: packEntity.title, packEntity },
        );
      });

      return [entityType, [...rows.values()]];
    }),
  );
};

export const entityForSource = (
  entity: CatalogEntity | undefined,
  source: EntitySource,
): EntityIndex | Entity | undefined => (source === SERVER_SOURCE ? entity?.serverEntity : entity?.packEntity);

/* The previous revision's copy is the default so that saving without any changes reproduces that revision. */
export const defaultEntity = (entity: CatalogEntity): EntityIndex | Entity =>
  entity.packEntity ?? entity.serverEntity;

export const sourceOf = (entity: EntityIndex | Entity): EntitySource =>
  entity && entity instanceof EntityIndex ? SERVER_SOURCE : CONTENT_PACK_SOURCE;

export const hasBothSources = (entity: CatalogEntity): boolean => !!entity.serverEntity && !!entity.packEntity;

/*
 * A row's two copies carry different ids, so a selected entity belongs to a row when it is either of them rather
 * than when the ids are equal.
 */
export const isEntityOfRow = (entity: CatalogEntity, selected: { id: string } | undefined): boolean =>
  !!selected && (selected.id === entity.serverEntity?.id || selected.id === entity.packEntity?.id);

export const rowEntityIds = (entity: CatalogEntity): Array<string> =>
  [entity.serverEntity?.id, entity.packEntity?.id].filter(Boolean);

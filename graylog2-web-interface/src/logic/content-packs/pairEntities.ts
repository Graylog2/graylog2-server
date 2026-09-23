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
import type Entity from 'logic/content-packs/Entity';
import type EntityIndex from 'logic/content-packs/EntityIndex';

type InstallationEntity = { id: string; content_pack_entity_id: string };
export type Installation = { content_pack_revision: number; entities?: Array<InstallationEntity> };
export type EntityPair = { packEntity: Entity; installedEntity: EntityIndex };
export type EntitySource = 'latest' | 'older';
type Selection = { [type: string]: Array<Entity | EntityIndex> };

const PREFERRED_RANK = Number.MAX_SAFE_INTEGER;

const orderInstallations = (installations: Array<Installation>, revision: number) => {
  const rank = (installation: Installation) =>
    installation.content_pack_revision === revision ? PREFERRED_RANK : installation.content_pack_revision;

  return [...installations].sort((a, b) => rank(b) - rank(a));
};

// Server and pack copies only share an id through the installation record, so entities
// without a mapping (or whose installed copy was deleted) stay unpaired.
export const pairEntities = (
  packEntities: Array<Entity>,
  entityIndex: { [type: string]: Array<EntityIndex> },
  installations: Array<Installation>,
  revision: number,
): Array<EntityPair> => {
  const installedEntities = orderInstallations(installations, revision).flatMap(({ entities = [] }) => entities);

  return packEntities.flatMap((packEntity) => {
    const catalogEntries = entityIndex[packEntity.type.name] ?? [];
    const installedEntity = installedEntities
      .filter(({ content_pack_entity_id }) => content_pack_entity_id === packEntity.id)
      .map(({ id }) => catalogEntries.find((catalogEntry) => catalogEntry.id === id))
      .find(Boolean);

    return installedEntity ? [{ packEntity, installedEntity }] : [];
  });
};

const isSelected = (selection: Selection, entity: Entity | EntityIndex) =>
  (selection[entity.type.name] ?? []).some((selected) => selected.id === entity.id);

export const switchSource = (selection: Selection, pairs: Array<EntityPair>, source: EntitySource): Selection =>
  pairs.reduce((result, { packEntity, installedEntity }) => {
    const [from, to] = source === 'latest' ? [packEntity, installedEntity] : [installedEntity, packEntity];

    // Checked against the original selection: two pack entities can share one installed copy, and the first swap
    // would otherwise hide the second.
    if (!isSelected(selection, from)) {
      return result;
    }

    const typeName = from.type.name;
    const remaining = (result[typeName] ?? []).filter((entity) => entity.id !== from.id);

    return { ...result, [typeName]: isSelected(result, to) ? remaining : [...remaining, to] };
  }, selection);

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
import Entity from 'logic/content-packs/Entity';
import EntityIndex from 'logic/content-packs/EntityIndex';
import { installedEntityIds, mergeEntityCatalog } from 'logic/content-packs/EntityCatalog';

const chainType = { name: 'detection_chain', version: '1' };

const serverEntity = (id: string, title: string) => EntityIndex.create(id, title, chainType as any);

const packEntity = (id: string, title: string) =>
  Entity.builder()
    .v('1')
    .type(chainType)
    .id(id)
    .data({ title: { '@value': title, '@type': 'string' } })
    .build();

describe('installedEntityIds', () => {
  it('maps pack entity ids to the ids they were installed as', () => {
    const installations = [
      {
        content_pack_revision: 3,
        entities: [{ id: 'server-1', content_pack_entity_id: 'pack-1' }],
      },
    ];

    expect(installedEntityIds(installations, 3)).toEqual({ 'pack-1': 'server-1' });
  });

  it('prefers the installation of the revision being edited', () => {
    const installations = [
      {
        content_pack_revision: 3,
        entities: [{ id: 'server-new', content_pack_entity_id: 'pack-1' }],
      },
      {
        content_pack_revision: 2,
        entities: [{ id: 'server-old', content_pack_entity_id: 'pack-1' }],
      },
    ];

    expect(installedEntityIds(installations, 3)).toEqual({ 'pack-1': 'server-new' });
  });

  it('returns nothing when the pack was never installed', () => {
    expect(installedEntityIds(undefined, 3)).toEqual({});
    expect(installedEntityIds([], 3)).toEqual({});
  });
});

describe('mergeEntityCatalog', () => {
  it('pairs the two copies of an entity through the installation mapping', () => {
    const server = serverEntity('server-1', 'Akira Ransomware');
    const pack = packEntity('pack-1', 'Akira Ransomware');

    const catalog = mergeEntityCatalog({ detection_chain: [server] }, [pack], { 'pack-1': 'server-1' });

    expect(catalog.detection_chain).toHaveLength(1);
    expect(catalog.detection_chain[0].serverEntity).toBe(server);
    expect(catalog.detection_chain[0].packEntity).toBe(pack);
  });

  it('leaves the copies separate when no mapping links them', () => {
    const catalog = mergeEntityCatalog({ detection_chain: [serverEntity('server-1', 'Akira Ransomware')] }, [
      packEntity('pack-1', 'Akira Ransomware'),
    ]);

    expect(catalog.detection_chain).toHaveLength(2);
  });

  it('keeps pack entity types the server catalog does not report', () => {
    const catalog = mergeEntityCatalog({}, [packEntity('pack-1', 'Akira Ransomware')]);

    expect(catalog.detection_chain).toHaveLength(1);
    expect(catalog.detection_chain[0].packEntity.id).toBe('pack-1');
  });

  it('prefers the server title, which reflects the entity as it is now', () => {
    const catalog = mergeEntityCatalog({ detection_chain: [serverEntity('server-1', 'Renamed on server')] }, [
      packEntity('pack-1', 'Old name in pack'),
    ], { 'pack-1': 'server-1' });

    expect(catalog.detection_chain[0].title).toBe('Renamed on server');
  });
});

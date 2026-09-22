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

import { pairEntities, selectedSource, switchSource } from './pairEntities';

const streamType = { name: 'stream', version: '1' };

const packEntity = (id: string) =>
  Entity.fromJSON({ v: '1', type: streamType as any, id, data: {} as any, constraints: [] }, false);
const catalogEntry = (id: string) => EntityIndex.create(id, 'Stream', streamType as any);
const installation = (revision: number, serverId: string, packId: string) => ({
  content_pack_revision: revision,
  entities: [{ id: serverId, content_pack_entity_id: packId }],
});

const packStream = packEntity('pack-stream-uuid');
const serverStream = catalogEntry('5f0000000000000000000001');
const entityIndex = { stream: [serverStream] };

describe('pairEntities', () => {
  it('pairs a pack entity with its installed copy', () => {
    expect(pairEntities([packStream], entityIndex, [installation(1, serverStream.id, packStream.id)], 1)).toEqual([
      { packEntity: packStream, installedEntity: serverStream },
    ]);
  });

  it('leaves entities without an installation mapping unpaired', () => {
    expect(pairEntities([packStream], entityIndex, [], 1)).toEqual([]);
  });

  it('leaves entities whose installed copy is no longer in the catalog unpaired', () => {
    expect(pairEntities([packStream], entityIndex, [installation(1, 'deleted-id', packStream.id)], 1)).toEqual([]);
  });

  it('prefers the installation of the edited revision', () => {
    const otherServerStream = catalogEntry('5f0000000000000000000002');
    const installations = [installation(3, otherServerStream.id, packStream.id), installation(2, serverStream.id, packStream.id)];

    expect(pairEntities([packStream], { stream: [serverStream, otherServerStream] }, installations, 2)).toEqual([
      { packEntity: packStream, installedEntity: serverStream },
    ]);
  });

  it('falls back to the installation of another revision', () => {
    expect(pairEntities([packStream], entityIndex, [installation(1, serverStream.id, packStream.id)], 2)).toEqual([
      { packEntity: packStream, installedEntity: serverStream },
    ]);
  });
});

describe('switchSource and selectedSource', () => {
  const pairedPack = packEntity('paired-pack-uuid');
  const pairedServer = catalogEntry('5f00000000000000000000aa');
  const uncheckedPack = packEntity('unchecked-pack-uuid');
  const uncheckedServer = catalogEntry('5f00000000000000000000bb');
  const unpairedPack = packEntity('unpaired-pack-uuid');
  const pairs = [
    { packEntity: pairedPack, installedEntity: pairedServer },
    { packEntity: uncheckedPack, installedEntity: uncheckedServer },
  ];
  const formerRevisionSelection = { stream: [pairedPack, unpairedPack] };

  it('reports the shared source of the checked, paired entities', () => {
    expect(selectedSource(formerRevisionSelection, pairs)).toBe('former_revision');
    expect(selectedSource({ stream: [pairedServer] }, pairs)).toBe('server');
  });

  it('reports no source when the checked, paired entities are mixed', () => {
    expect(selectedSource({ stream: [pairedServer, uncheckedPack] }, pairs)).toBe('');
  });

  it('swaps only the checked, paired entities to their installed copies for Server', () => {
    expect(switchSource(formerRevisionSelection, pairs, 'server')).toEqual({ stream: [unpairedPack, pairedServer] });
  });

  it('swaps them back for Former revision', () => {
    const serverSelection = switchSource(formerRevisionSelection, pairs, 'server');

    expect(switchSource(serverSelection, pairs, 'former_revision')).toEqual({ stream: [unpairedPack, pairedPack] });
  });
});

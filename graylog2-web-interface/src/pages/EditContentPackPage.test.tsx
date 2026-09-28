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
import * as React from 'react';
import { render, waitFor } from 'wrappedTestingLibrary';

import { asMock } from 'helpers/mocking';
import ContentPack from 'logic/content-packs/ContentPack';
import Entity from 'logic/content-packs/Entity';
import EntityIndex from 'logic/content-packs/EntityIndex';
import ContentPackEdit from 'components/content-packs/ContentPackEdit';
import useEntityIndex from 'components/content-packs/hooks/useEntityIndex';
import useContentPackRevisions from 'components/content-packs/hooks/useContentPackRevisions';
import useContentPackInstallations from 'components/content-packs/hooks/useContentPackInstallations';

import EditContentPackPage from './EditContentPackPage';

jest.mock('routing/useParams', () => () => ({ contentPackId: 'pack-id', contentPackRev: '1' }));
jest.mock('components/content-packs/hooks/useEntityIndex');
jest.mock('components/content-packs/hooks/useContentPackRevisions');
jest.mock('components/content-packs/hooks/useContentPackInstallations');
jest.mock('components/content-packs/ContentPackEdit', () => jest.fn(() => null));

const type = { name: 'stream', version: '1' };
const packStream = Entity.fromJSON(
  { v: '1', type: type as any, id: 'pack-stream-uuid', data: {} as any, constraints: [] },
  false,
);
const serverStream = EntityIndex.create('5f0000000000000000000001', 'Stream', type as any);

describe('EditContentPackPage', () => {
  beforeEach(() => {
    const contentPack = ContentPack.builder().id('pack-id').rev(2).entities([packStream]).build();

    asMock(useEntityIndex).mockReturnValue({ entityIndex: { stream: [serverStream] }, isLoading: false });
    asMock(useContentPackRevisions).mockReturnValue({
      data: { contentPackRevisions: { createNewVersionFromRev: () => contentPack } },
    } as any);
    asMock(useContentPackInstallations).mockReturnValue({
      data: {
        installations: [
          { content_pack_revision: 1, entities: [{ id: serverStream.id, content_pack_entity_id: packStream.id }] },
        ],
      },
    } as any);
  });

  it('keeps the pack copy selected by default and passes the pair on', async () => {
    render(<EditContentPackPage />);

    await waitFor(() =>
      expect((ContentPackEdit as unknown as jest.Mock).mock.lastCall[0]).toEqual(
        expect.objectContaining({
          selectedEntities: { stream: [packStream] },
          entityPairs: [{ packEntity: packStream, installedEntity: serverStream }],
        }),
      ),
    );
  });
});

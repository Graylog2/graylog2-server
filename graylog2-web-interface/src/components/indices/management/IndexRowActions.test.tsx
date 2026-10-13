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
import { render, screen } from 'wrappedTestingLibrary';

import asMock from 'helpers/mocking/AsMock';
import { adminUser } from 'fixtures/users';
import useCurrentUser from 'hooks/useCurrentUser';

import IndexRowActions from './IndexRowActions';
import type { IndexRow } from './fetchIndices';

jest.mock('hooks/useCurrentUser');
jest.mock('@graylog/server-api', () => ({
  IndexerIndicesManagementActions: {},
  ClusterDeflector: {},
}));

const row = (overrides: Partial<IndexRow> = {}): IndexRow => ({
  id: 'graylog_17',
  index: 'graylog_17',
  health: 'green',
  status: 'open',
  tier: 'hot',
  primary_shards: 1,
  replicas: 0,
  docs_count: 10,
  store_size_bytes: 208,
  index_set_id: 'set-1',
  index_set_title: 'Default index set',
  is_write_index: false,
  ...overrides,
});

describe('IndexRowActions', () => {
  beforeEach(() => {
    asMock(useCurrentUser).mockReturnValue(adminUser);
  });

  it('searches all messages of the index', async () => {
    render(<IndexRowActions index={row()} />);

    const search = await screen.findByRole('link', { name: /search in index graylog_17/i });

    expect(decodeURIComponent(search.getAttribute('href'))).toBe('/search?q=_index:graylog_17&relative=0');
  });

  it("doesn't search a closed index", async () => {
    render(<IndexRowActions index={row({ status: 'close' })} />);

    expect(await screen.findByRole('button', { name: /graylog_17 is closed/i })).toBeDisabled();
  });

  it('offers no search to users who may not search', () => {
    asMock(useCurrentUser).mockReturnValue(adminUser.toBuilder().permissions(['indices:read']).build());
    render(<IndexRowActions index={row()} />);

    expect(screen.queryByRole('link', { name: /search in index/i })).not.toBeInTheDocument();
  });
});

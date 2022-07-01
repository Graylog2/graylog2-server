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
import { BrowserRouter as Router } from 'react-router-dom';

import LUTTableEntry from './LUTTableEntry';
import type { Table, Cache, DataAdapter } from './LUTTableEntry';

const TABLE: Table = {
  id: '62a9e6bdf3d7456348ef8e53',
  title: 'Watchlist (Internal - Do not remove)',
  description: 'A basic description',
  name: 'watchlist',
  _metadata: null,
};

const CACHE: Cache = {
  id: '62a9e6bdf3d7456348ef8e51',
  title: 'Watchlist (Internal - Do not remove)',
  description: 'A cache for the Watchlist entries to speed up lookup.',
  name: 'watchlist-cache',
  _metadata: null,
};

const DATA_ADAPTER: DataAdapter = {
  id: '62a9e6bdf3d7456348ef8e4f',
  title: 'Watchlist (Internal - Do not remove)',
  description: 'The Lookup Adapter for the Watchlist.',
  name: 'watchlist-mongo',
  _metadata: null,
};

const renderedLUT = (scope: string) => {
  TABLE._metadata = {
    scope: scope,
    revision: 2,
    created_at: '2022-06-13T08:47:12Z',
    updated_at: '2022-06-29T12:00:28Z',
  };

  return render(
    <Router>
      <LUTTableEntry table={TABLE} cache={CACHE} dataAdapter={DATA_ADAPTER} />
    </Router>,
    {
      container: document.body.appendChild(document.createElement('table')),
    },
  );
};

describe('LUTTableEntry', () => {
  it('should show "edit" button', async () => {
    const { getByRole } = renderedLUT('DEFAULT');

    let actionBtn = null;
    await waitFor(() => { actionBtn = getByRole('edit-button'); });

    expect(actionBtn).toBeVisible();
  });

  it('should not show "edit" button', async () => {
    const { queryByRole } = renderedLUT('ILLUMINATE');

    let actionBtn = null;
    await waitFor(() => { actionBtn = queryByRole('edit-button'); });

    expect(actionBtn).toBeNull();
  });

  it('should show "delete" button', async () => {
    const { getByRole } = renderedLUT('DEFAULT');

    let actionBtn = null;
    await waitFor(() => { actionBtn = getByRole('delete-button'); });

    expect(actionBtn).toBeVisible();
  });

  it('should not show "delete" button', async () => {
    const { queryByRole } = renderedLUT('ILLUMINATE');

    let actionBtn = null;
    await waitFor(() => { actionBtn = queryByRole('delete-button'); });

    expect(actionBtn).toBeNull();
  });
});

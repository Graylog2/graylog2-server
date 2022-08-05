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
import { BrowserRouter as Router } from 'react-router-dom';

import { asMock } from 'helpers/mocking';

import { DATA_ADAPTER, mockedUseScopePermissions } from './fixtures';
import DataAdapterTableEntry from './DataAdapterTableEntry';

jest.mock('hooks/useScopePermissions', () => (mockedUseScopePermissions));

const renderedDataAdapter = (scope: string) => {
  const auxDataAdapter = { ...DATA_ADAPTER };
  auxDataAdapter._scope = scope;

  return render(
    <Router>
      <DataAdapterTableEntry adapter={auxDataAdapter} error={null} />
    </Router>,
    {
      container: document.body.appendChild(document.createElement('table')),
    },
  );
};

describe('DataAdapterTableEntry', () => {
  it('should show Loading spinner while loading scope permissions', async () => {
    asMock(mockedUseScopePermissions).mockReturnValueOnce({
      loadingScopePermissions: true,
      scopePermissions: null,
    });

    renderedDataAdapter('DEFAULT');

    expect(screen.queryByRole('button', { name: /edit/i })).not.toBeInTheDocument();
    expect(screen.getByText('Loading...')).toBeInTheDocument();
  });

  it('should show "edit" button for non ILLUMINATE entities', async () => {
    renderedDataAdapter('DEFAULT');

    expect(screen.getByRole('button', { name: /edit/i })).toBeInTheDocument();
  });

  it('should disable "edit" button for ILLUMINATE entities', async () => {
    renderedDataAdapter('ILLUMINATE');

    expect(screen.queryByRole('button', { name: /edit/i })).not.toBeInTheDocument();
  });

  it('should show "delete" button for non ILLUMINATE entities', async () => {
    renderedDataAdapter('DEFAULT');

    expect(screen.getByRole('button', { name: /delete/i })).toBeInTheDocument();
  });

  it('should disable "delete" button for ILLUMINATE entities', async () => {
    renderedDataAdapter('ILLUMINATE');

    expect(screen.queryByRole('button', { name: /delete/i })).not.toBeInTheDocument();
  });
});

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

import { asMock } from 'helpers/mocking';

import { DATA_ADAPTER, mockedUseScopePermissions } from './fixtures';
import DataAdapterTableEntry from './DataAdapterTableEntry';

jest.mock('hooks/useScopePermissions', () => (mockedUseScopePermissions));

const renderedDataAdapter = (scope: string) => {
  DATA_ADAPTER._scope = scope;

  return render(
    <Router>
      <DataAdapterTableEntry adapter={DATA_ADAPTER} error={null} />
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

    const { queryByAltText, queryByText } = renderedDataAdapter('DEFAULT');

    await waitFor(() => {
      expect(queryByAltText('edit button')).toBeNull();
    });

    await waitFor(() => {
      expect(queryByText('Loading...')).toBeVisible();
    });
  });

  it('should show "edit" button for non ILLUMINATE entities', async () => {
    const { baseElement } = renderedDataAdapter('DEFAULT');

    await waitFor(() => {
      expect(baseElement.querySelector('button[alt="edit button"]')).toBeVisible();
    });
  });

  it('should disable "edit" button for ILLUMINATE entities', async () => {
    const { queryByAltText } = renderedDataAdapter('ILLUMINATE');

    await waitFor(() => {
      expect(queryByAltText('edit button')).toBeNull();
    });
  });

  it('should show "delete" button for non ILLUMINATE entities', async () => {
    const { baseElement } = renderedDataAdapter('DEFAULT');

    await waitFor(() => {
      expect(baseElement.querySelector('button[alt="delete button"]')).toBeVisible();
    });
  });

  it('should disable "delete" button for ILLUMINATE entities', async () => {
    const { queryByAltText } = renderedDataAdapter('ILLUMINATE');

    await waitFor(() => {
      expect(queryByAltText('delete button')).toBeNull();
    });
  });
});

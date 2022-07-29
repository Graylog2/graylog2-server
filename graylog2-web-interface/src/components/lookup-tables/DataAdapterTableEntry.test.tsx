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
import { exampleEntityScope } from 'fixtures/entityScope';
import fetchScopePermissions from 'hooks/api/fetchScopePermissions';

import { DATA_ADAPTER } from './fixtures';
import DataAdapterTableEntry from './DataAdapterTableEntry';

jest.mock('hooks/api/fetchScopePermissions', () => jest.fn());

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
  it('should show "edit" button for non ILLUMINATE entities', async () => {
    asMock(fetchScopePermissions).mockResolvedValueOnce(exampleEntityScope);

    const { getByTestId } = renderedDataAdapter('DEFAULT');

    await waitFor(() => {
      expect(getByTestId('edit-button')).not.toBeDisabled();
    });
  });

  it('should disable "edit" button for ILLUMINATE entities', async () => {
    asMock(fetchScopePermissions).mockResolvedValueOnce(exampleEntityScope);

    const { getByTestId } = renderedDataAdapter('ILLUMINATE');

    await waitFor(() => {
      expect(getByTestId('edit-button')).toHaveClass('disabled');
    });
  });

  it('should show "delete" button for non ILLUMINATE entities', async () => {
    asMock(fetchScopePermissions).mockResolvedValueOnce(exampleEntityScope);

    const { getByTestId } = renderedDataAdapter('DEFAULT');

    await waitFor(() => {
      expect(getByTestId('delete-button')).not.toBeDisabled();
    });
  });

  it('should disable "delete" button for ILLUMINATE entities', async () => {
    asMock(fetchScopePermissions).mockResolvedValueOnce(exampleEntityScope);

    const { getByTestId } = renderedDataAdapter('ILLUMINATE');

    await waitFor(() => {
      expect(getByTestId('delete-button')).toBeDisabled();
    });
  });
});

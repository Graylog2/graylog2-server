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

import { DATA_ADAPTER } from './fixtures';
import DataAdapterTableEntry from './DataAdapterTableEntry';

const renderedDataAdapter = (scope: string) => {
  DATA_ADAPTER._metadata = {
    scope: scope,
    revision: 2,
    created_at: '2022-06-13T08:47:12Z',
    updated_at: '2022-06-29T12:00:28Z',
  };

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
    const { getByTestId } = renderedDataAdapter('DEFAULT');

    let actionBtn = null;
    await waitFor(() => { actionBtn = getByTestId('edit-button'); });

    expect(actionBtn).not.toBeDisabled();
  });

  it('should not show "edit" button for ILLUMINATE entities', async () => {
    const { getByTestId } = renderedDataAdapter('ILLUMINATE');

    let actionBtn = null;
    await waitFor(() => { actionBtn = getByTestId('edit-button'); });

    expect(actionBtn).toBeDisabled();
  });

  it('should show "delete" button for non ILLUMINATE entities', async () => {
    const { getByTestId } = renderedDataAdapter('DEFAULT');

    let actionBtn = null;
    await waitFor(() => { actionBtn = getByTestId('delete-button'); });

    expect(actionBtn).not.toBeDisabled();
  });

  it('should not show "delete" button for ILLUMINATE entities', async () => {
    const { getByTestId } = renderedDataAdapter('ILLUMINATE');

    let actionBtn = null;
    await waitFor(() => { actionBtn = getByTestId('delete-button'); });

    expect(actionBtn).toBeDisabled();
  });
});

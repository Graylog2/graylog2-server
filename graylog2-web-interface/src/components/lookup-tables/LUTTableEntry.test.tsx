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

import { TABLE, CACHE, DATA_ADAPTER } from './fixtures';
import LUTTableEntry from './LUTTableEntry';

const renderedLUT = (scope: string) => {
  TABLE.scope = scope;

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
    const { baseElement } = renderedLUT('DEFAULT');

    await waitFor(() => {
      const actionBtn = baseElement.querySelector('button[alt="edit button"]');

      expect(actionBtn).toBeVisible();
    });
  });

  it('should not show "edit" button', async () => {
    const { baseElement } = renderedLUT('ILLUMINATE');

    await waitFor(() => {
      const actionBtn = baseElement.querySelector('button[alt="edit button"]');

      expect(actionBtn).toBeNull();
    });
  });

  it('should show "delete" button', async () => {
    const { baseElement } = renderedLUT('DEFAULT');

    await waitFor(() => {
      const actionBtn = baseElement.querySelector('button[alt="delete button"]');

      expect(actionBtn).toBeVisible();
    });
  });

  it('should not show "delete" button', async () => {
    const { baseElement } = renderedLUT('ILLUMINATE');

    await waitFor(() => {
      const actionBtn = baseElement.querySelector('button[alt="delete button"]');

      expect(actionBtn).toBeNull();
    });
  });
});

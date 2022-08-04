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

import { CACHE, mockedUseScopePermissions } from './fixtures';
import CacheTableEntry from './CacheTableEntry';

jest.mock('hooks/useScopePermissions', () => (mockedUseScopePermissions));

const renderedCTE = (scope: string) => {
  CACHE._scope = scope;

  return render(
    <Router><CacheTableEntry cache={CACHE} /></Router>,
    {
      container: document.body.appendChild(document.createElement('table')),
    },
  );
};

describe('CacheTableEntry', () => {
  it('should show Loading spinner while loading scope permissions', async () => {
    asMock(mockedUseScopePermissions).mockReturnValueOnce({
      loadingScopePermissions: true,
      scopePermissions: null,
    });

    const { queryByAltText, queryByText } = renderedCTE('DEFAULT');

    await waitFor(() => {
      expect(queryByAltText('edit button')).toBeNull();
    });

    await waitFor(() => {
      expect(queryByText('Loading...')).toBeVisible();
    });
  });

  it('should show "edit" button', async () => {
    const { baseElement } = renderedCTE('DEFAULT');

    await waitFor(() => {
      expect(baseElement.querySelector('button[alt="edit button"]')).toBeVisible();
    });
  });

  it('should not show "edit" button', async () => {
    const { queryByAltText } = renderedCTE('ILLUMINATE');

    await waitFor(() => {
      expect(queryByAltText('edit button')).toBeNull();
    });
  });

  it('should show "delete" button', async () => {
    const { baseElement } = renderedCTE('DEFAULT');

    await waitFor(() => {
      expect(baseElement.querySelector('button[alt="delete button"]')).toBeVisible();
    });
  });

  it('should not show "delete" button', async () => {
    const { queryByAltText } = renderedCTE('ILLUMINATE');

    await waitFor(() => {
      expect(queryByAltText('delete button')).toBeNull();
    });
  });
});

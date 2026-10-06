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
import * as mockImmutable from 'immutable';
import { render, screen } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';

import OriginalViewHeader from 'views/components/views/ViewHeader';
import TestStoreProvider from 'views/test/TestStoreProvider';
import useViewsPlugin from 'views/test/testViewsPlugin';
import { createSearch } from 'fixtures/searches';
import View from 'views/logic/views/View';
import onSaveView from 'views/logic/views/OnSaveViewAction';
import { updateView } from 'views/logic/slices/viewSlice';
import asMock from 'helpers/mocking/AsMock';
import { createEntityShareState } from 'fixtures/entityShareState';
import useEntityShareState from 'hooks/useEntityShareState';
import useScopePermissions from 'hooks/useScopePermissions';

jest.mock('views/logic/views/OnSaveViewAction');
jest.mock('hooks/useScopePermissions');

jest.mock('views/logic/slices/viewSlice', () => {
  const actualModule = jest.requireActual('views/logic/slices/viewSlice');

  return {
    ...actualModule,
    updateView: jest.fn(actualModule.updateView),
  };
});
jest.mock('api/entity-share', () => ({
  prepareEntityShare: jest.fn(() => Promise.resolve()),
  updateEntityShare: jest.fn(() => Promise.resolve()),
  loadUserSharesPaginated: jest.fn(() =>
    Promise.resolve({
      list: mockImmutable.List(),
      pagination: { page: 1, perPage: 10, query: '', total: 0, count: 0 },
    }),
  ),
}));
jest.mock('hooks/useEntityShareState', () => {
  const mockSetEntityShareState = jest.fn();

  return {
    __esModule: true,
    default: jest.fn(() => ({ data: undefined })),
    useSetEntityShareState: jest.fn(() => mockSetEntityShareState),
    entityShareQueryKey: jest.fn((grn) => ['entity-share', grn ?? 'new']),
  };
});
const view = createSearch().toBuilder().id('viewId').title('Some view').type(View.Type.Dashboard).build();

const ViewHeader = () => (
  <TestStoreProvider view={view}>
    <OriginalViewHeader />
  </TestStoreProvider>
);

const setupUser = () => userEvent.setup({ advanceTimers: jest.advanceTimersByTime });

describe('ViewHeader', () => {
  beforeEach(() => {
    asMock(onSaveView).mockReturnValue(async () => {});
    asMock(useEntityShareState).mockReturnValue({ data: createEntityShareState } as any);
    asMock(useScopePermissions).mockReturnValue({
      loadingScopePermissions: false,
      scopePermissions: { is_mutable: true, is_deletable: true },
      checkPermissions: () => true,
    });
  });

  beforeAll(() => {
    jest.useFakeTimers();
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  useViewsPlugin();

  it('Render view type and title', async () => {
    render(<ViewHeader />);

    await screen.findByText('Dashboards', { exact: false });
    await screen.findByText('Some view');
  });

  it('Updates view with new title', async () => {
    render(<ViewHeader />);

    const editButton = await screen.findByTitle('Edit dashboard Some view metadata');

    await setupUser().click(editButton);
    await screen.findByText('Editing saved dashboard', { exact: false });

    const titleInput = await screen.findByRole('textbox', { name: /title/i });
    await setupUser().type(titleInput, ' updated');

    const saveButton = await screen.findByRole('button', { name: /save dashboard/i });
    await setupUser().click(saveButton);

    expect(onSaveView).toHaveBeenCalledWith(expect.objectContaining({ title: 'Some view updated' }));
    expect(updateView).toHaveBeenCalledWith(expect.objectContaining({ title: 'Some view updated' }));
  });

  it('does not offer editing metadata of views with an immutable scope', async () => {
    asMock(useScopePermissions).mockReturnValue({
      loadingScopePermissions: false,
      scopePermissions: { is_mutable: false, is_deletable: false },
      checkPermissions: () => false,
    });

    render(<ViewHeader />);

    await screen.findByText('Some view');

    expect(screen.queryByTitle('Edit dashboard Some view metadata')).not.toBeInTheDocument();
  });
});

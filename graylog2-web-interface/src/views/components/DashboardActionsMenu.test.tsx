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
import React from 'react';
import * as mockImmutable from 'immutable';
import { render, screen, within, waitFor } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';

import { asMock } from 'helpers/mocking';
import { adminUser } from 'fixtures/users';
import type { LayoutState } from 'views/components/contexts/SearchPageLayoutContext';
import Search from 'views/logic/search/Search';
import View from 'views/logic/views/View';
import { SAVE_COPY, BLANK } from 'views/components/contexts/SearchPageLayoutContext';
import { ViewManagementActions } from 'views/stores/ViewManagementStore';
import useSaveViewFormControls from 'views/hooks/useSaveViewFormControls';
import useCurrentUser from 'hooks/useCurrentUser';
import TestStoreProvider from 'views/test/TestStoreProvider';
import { loadViewsPlugin, unloadViewsPlugin } from 'views/test/testViewsPlugin';
import OnSaveViewAction from 'views/logic/views/OnSaveViewAction';
import HotkeysProvider from 'contexts/HotkeysProvider';
import SearchPageLayoutProvider from 'views/components/contexts/SearchPageLayoutProvider';

import DashboardActionsMenu from './DashboardActionsMenu';

jest.mock('views/logic/views/OnSaveViewAction', () => jest.fn(() => () => {}));
jest.mock('views/hooks/useSaveViewFormControls');
jest.mock('hooks/useCurrentUser');
jest.mock('hooks/useFeature', () => (featureFlag: string) => featureFlag === 'frontend_hotkeys');

jest.mock('bson-objectid', () => jest.fn(() => ({
  toString: jest.fn(() => 'new-dashboard-id'),
})));

jest.mock('views/stores/ViewManagementStore', () => ({
  ViewManagementActions: {
    create: jest.fn((v) => Promise.resolve(v)).mockName('create'),
  },
}));

jest.mock('stores/permissions/EntityShareStore', () => ({
  EntityShareActions: {
    prepare: jest.fn(() => Promise.resolve()),
    update: jest.fn(() => Promise.resolve()),
  },
  EntityShareStore: {
    listen: jest.fn(),
    getInitialState: jest.fn(() => ({ state: undefined })),
  },
}));

describe('DashboardActionsMenu', () => {
  const mockView = View.create().toBuilder().id('view-id').type(View.Type.Dashboard)
    .search(Search.builder().build())
    .title('View title')
    .createdAt(new Date('2019-10-16T14:38:44.681Z'))
    .build();

  const SUT = ({ providerOverrides, view }: { providerOverrides?: Partial<LayoutState>, view?: View }) => (
    <TestStoreProvider view={view}>
      <HotkeysProvider>
        <SearchPageLayoutProvider value={providerOverrides}>
          <DashboardActionsMenu />
        </SearchPageLayoutProvider>
      </HotkeysProvider>
    </TestStoreProvider>
  );

  SUT.defaultProps = {
    providerOverrides: undefined,
    view: mockView,
  };

  beforeAll(loadViewsPlugin);

  afterAll(unloadViewsPlugin);

  const submitDashboardSaveForm = async () => {
    const saveDashboardModal = await screen.findByTestId('modal-form');

    const saveButton = within(saveDashboardModal).getByRole('button', {
      name: /create dashboard/i,
      hidden: true,
    });

    userEvent.click(saveButton);
  };

  const openDashboardSaveForm = async () => {
    const saveAsMenuItem = await screen.findByRole('button', { name: /save as new dashboard/i });

    userEvent.click(saveAsMenuItem);
  };

  beforeEach(() => {
    asMock(useCurrentUser).mockReturnValue(adminUser.toBuilder()
      .grnPermissions(mockImmutable.List(['entity:own:grn::::dashboard:view-id']))
      .permissions(mockImmutable.List(['dashboards:edit:view-id', 'view:edit:view-id']))
      .build());

    asMock(useSaveViewFormControls).mockReturnValue([]);
  });

  it('should save a new dashboard', async () => {
    render(<SUT view={mockView.toBuilder().id(undefined).build()} />);

    await openDashboardSaveForm();
    await submitDashboardSaveForm();

    const updatedDashboard = mockView.toBuilder().id('new-dashboard-id').build();

    await waitFor(() => expect(ViewManagementActions.create).toHaveBeenCalledWith(updatedDashboard));
  });

  it('should extend a dashboard with plugin data on duplication', async () => {
    asMock(useSaveViewFormControls).mockReturnValue([{
      component: () => <div>Pluggable component!</div>,
      id: 'example-plugin-component',
      onDashboardDuplication: (view: View) => Promise.resolve(view.toBuilder().summary('This dashboard has been extended by a plugin').build()),
    }],
    );

    render(<SUT view={mockView} />);

    await openDashboardSaveForm();
    await submitDashboardSaveForm();

    const updatedDashboard = mockView.toBuilder().id('new-dashboard-id').summary('This dashboard has been extended by a plugin').build();

    await waitFor(() => expect(ViewManagementActions.create).toHaveBeenCalledWith(updatedDashboard));
  });

  it('should open edit dashboard meta information modal', async () => {
    const { getByText, findByText } = render(<SUT />);
    const editMenuItem = getByText(/Edit metadata/i);

    userEvent.click(editMenuItem);

    await findByText(/Editing dashboard/);
  });

  it('should open dashboard share modal', () => {
    const { getByRole, getByText } = render(<SUT />);
    const openShareButton = getByText(/Share/i);

    userEvent.click(openShareButton);

    expect(getByRole('button', { name: /update sharing/i, hidden: true })).not.toBeNull();
  });

  it('should use FULL_MENU layout option by default and render all buttons', async () => {
    const { findByRole, findByTitle } = render(<SUT />);

    await findByTitle(/Save dashboard/);
    await findByTitle(/Save as new dashboard/);
    await findByTitle(/Share/);
    await findByRole(/^menu$/);
  });

  it('should only render "Save As" button in SAVE_COPY layout option', async () => {
    const { findByTitle, queryByRole, queryByTitle } = render(<SUT providerOverrides={{ sidebar: { isShown: false }, viewActions: SAVE_COPY }} />);

    const saveButton = queryByTitle(/Save dashboard/);
    const shareButton = queryByTitle(/Share/);
    const extrasButton = queryByRole(/^menu$/);

    expect(saveButton).not.toBeInTheDocument();
    expect(shareButton).not.toBeInTheDocument();
    expect(extrasButton).not.toBeInTheDocument();

    await findByTitle(/Save as new dashboard/);
  });

  it('should render no action menu items in BLANK layout option', () => {
    const { queryByRole, queryByTitle } = render(<SUT providerOverrides={{ sidebar: { isShown: false }, viewActions: BLANK }} />);

    const saveButton = queryByTitle(/Save dashboard/);
    const saveAsButton = queryByTitle(/Save as new dashboard/);
    const shareButton = queryByTitle(/Share/);
    const extrasButton = queryByRole(/^menu$/);

    expect(saveButton).not.toBeInTheDocument();
    expect(saveAsButton).not.toBeInTheDocument();
    expect(shareButton).not.toBeInTheDocument();
    expect(extrasButton).not.toBeInTheDocument();
  });

  it('should save view when pressing related keyboard shortcut', async () => {
    render(<SUT />);
    userEvent.keyboard('{Meta>}s{/Meta}');
    await waitFor(() => expect(OnSaveViewAction).toHaveBeenCalledTimes(1));
  });
});

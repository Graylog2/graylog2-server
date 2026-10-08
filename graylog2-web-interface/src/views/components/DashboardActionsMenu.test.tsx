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
import Immutable from 'immutable';

import { asMock } from 'helpers/mocking';
import { adminUser } from 'fixtures/users';
import type { LayoutState } from 'views/components/contexts/SearchPageLayoutContext';
import Search from 'views/logic/search/Search';
import View from 'views/logic/views/View';
import { SAVE_COPY, BLANK } from 'views/components/contexts/SearchPageLayoutContext';
import useSaveViewFormControls from 'views/hooks/useSaveViewFormControls';
import useCurrentUser from 'hooks/useCurrentUser';
import useScopePermissions from 'hooks/useScopePermissions';
import TestStoreProvider from 'views/test/TestStoreProvider';
import useViewsPlugin from 'views/test/testViewsPlugin';
import OnSaveViewAction from 'views/logic/views/OnSaveViewAction';
import HotkeysProvider from 'contexts/HotkeysProvider';
import SearchPageLayoutProvider from 'views/components/contexts/SearchPageLayoutProvider';
import { createView } from 'views/api/views';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';

import DashboardActionsMenu from './DashboardActionsMenu';

jest.mock('views/logic/views/OnSaveViewAction', () => jest.fn(() => () => {}));
jest.mock('views/hooks/useSaveViewFormControls');
jest.mock('hooks/useCurrentUser');
jest.mock('hooks/useScopePermissions');
jest.mock('logic/telemetry/useSendTelemetry');
jest.mock('logic/generateObjectId', () => jest.fn(() => 'new-dashboard-id'));

jest.mock('views/api/views', () => ({
  createView: jest.fn((v) => Promise.resolve(v)).mockName('create'),
}));

jest.mock('api/entity-share', () => ({
  prepareEntityShare: jest.fn(() => Promise.resolve()),
  updateEntityShare: jest.fn(() => Promise.resolve()),
  loadUserSharesPaginated: jest.fn(() =>
    Promise.resolve({
      list: Immutable.List(),
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

describe('DashboardActionsMenu', () => {
  const mockView = View.create()
    .toBuilder()
    .id('view-id')
    .type(View.Type.Dashboard)
    .search(Search.builder().build())
    .title('View title')
    .createdAt(new Date('2019-10-16T14:38:44.681Z'))
    .build();

  const SUT = ({
    providerOverrides = undefined,
    view = mockView,
  }: {
    providerOverrides?: Partial<LayoutState>;
    view?: View;
  }) => (
    <TestStoreProvider view={view}>
      <HotkeysProvider>
        <SearchPageLayoutProvider value={providerOverrides}>
          <DashboardActionsMenu />
        </SearchPageLayoutProvider>
      </HotkeysProvider>
    </TestStoreProvider>
  );

  useViewsPlugin();

  const submitDashboardSaveForm = async () => {
    const saveDashboardModal = await screen.findByRole('dialog', { name: /Save new dashboard/i });

    const saveButton = within(saveDashboardModal).getByRole('button', {
      name: /create dashboard/i,
    });

    await userEvent.click(saveButton);
  };

  const openDashboardSaveForm = async () => {
    const saveAsMenuItem = await screen.findByRole('button', { name: /save as new dashboard/i });

    await userEvent.click(saveAsMenuItem);
  };

  const sendTelemetry = jest.fn();

  beforeEach(() => {
    sendTelemetry.mockClear();
    asMock(useSendTelemetry).mockReturnValue(sendTelemetry);
    asMock(useCurrentUser).mockReturnValue(
      adminUser
        .toBuilder()
        .grnPermissions(mockImmutable.List(['entity:own:grn::::dashboard:view-id']))
        .permissions(mockImmutable.List(['dashboards:edit:view-id', 'view:edit:view-id']))
        .build(),
    );

    asMock(useSaveViewFormControls).mockReturnValue([]);
    asMock(useScopePermissions).mockReturnValue({
      loadingScopePermissions: false,
      scopePermissions: { is_mutable: true, is_deletable: true },
      checkPermissions: () => true,
    });
  });

  it('should save a new dashboard', async () => {
    render(<SUT view={mockView.toBuilder().id(undefined).build()} />);

    await openDashboardSaveForm();
    await submitDashboardSaveForm();

    const updatedDashboard = mockView.toBuilder().id('new-dashboard-id').build();

    await waitFor(() => expect(createView).toHaveBeenCalledWith(updatedDashboard, null, undefined));
  });

  it('should extend a dashboard with plugin data on duplication', async () => {
    asMock(useSaveViewFormControls).mockReturnValue([
      {
        component: () => <div>Pluggable component!</div>,
        id: 'example-plugin-component',
        onDashboardDuplication: (view: View) =>
          Promise.resolve(view.toBuilder().summary('This dashboard has been extended by a plugin').build()),
      },
    ]);

    render(<SUT view={mockView} />);

    await openDashboardSaveForm();
    await submitDashboardSaveForm();

    const updatedDashboard = mockView
      .toBuilder()
      .id('new-dashboard-id')
      .summary('This dashboard has been extended by a plugin')
      .build();

    await waitFor(() => expect(createView).toHaveBeenCalledWith(updatedDashboard, null, 'view-id'));
  });

  it('does not send Illuminate clone telemetry when duplicating a non-Illuminate dashboard', async () => {
    render(<SUT view={mockView} />);

    await openDashboardSaveForm();
    await submitDashboardSaveForm();

    await waitFor(() => expect(createView).toHaveBeenCalled());

    expect(sendTelemetry).toHaveBeenCalledWith(
      TELEMETRY_EVENT_TYPE.DASHBOARD_ACTION.DASHBOARD_NEW_SAVED,
      expect.anything(),
    );
    expect(sendTelemetry).not.toHaveBeenCalledWith(
      TELEMETRY_EVENT_TYPE.DASHBOARD_ACTION.ILLUMINATE_DASHBOARD_CLONED,
      expect.anything(),
    );
  });

  it('should open edit dashboard meta information modal', async () => {
    const { findByText } = render(<SUT />);
    await userEvent.click(await screen.findByRole('button', { name: /more actions/i }));
    const editMenuItem = await screen.findByText(/Edit metadata/i);

    await userEvent.click(editMenuItem);

    await findByText(/Editing dashboard/);
  });

  it('should open dashboard share modal', async () => {
    render(<SUT />);
    const openShareButton = await screen.findByRole('button', { name: /Share/i });
    await userEvent.click(openShareButton);

    await screen.findByRole('button', { name: /update sharing/i });
  });

  it('should use FULL_MENU layout option by default and render all buttons', async () => {
    const { findByRole, findByTitle } = render(<SUT />);

    await findByTitle(/Save dashboard/);
    await findByTitle(/Save as new dashboard/);
    await findByRole('button', { name: /Share/i });
    await findByRole('button', { name: /more actions/i });
  });

  it('should only render "Save As" button in SAVE_COPY layout option', async () => {
    const { findByTitle, queryByRole, queryByTitle } = render(
      <SUT providerOverrides={{ sidebar: { isShown: false }, viewActions: SAVE_COPY }} />,
    );

    const saveButton = queryByTitle(/Save dashboard/);
    const shareButton = queryByRole('button', { name: /Share/i });
    const extrasButton = queryByRole('menu');

    expect(saveButton).not.toBeInTheDocument();
    expect(shareButton).not.toBeInTheDocument();
    expect(extrasButton).not.toBeInTheDocument();

    await findByTitle(/Save as new dashboard/);
  });

  it('should render no action menu items in BLANK layout option', () => {
    const { queryByRole, queryByTitle } = render(
      <SUT providerOverrides={{ sidebar: { isShown: false }, viewActions: BLANK }} />,
    );

    const saveButton = queryByTitle(/Save dashboard/);
    const saveAsButton = queryByTitle(/Save as new dashboard/);
    const shareButton = queryByRole('button', { name: /Share/i });
    const extrasButton = queryByRole('menu');

    expect(saveButton).not.toBeInTheDocument();
    expect(saveAsButton).not.toBeInTheDocument();
    expect(shareButton).not.toBeInTheDocument();
    expect(extrasButton).not.toBeInTheDocument();
  });

  it('should save view when pressing related keyboard shortcut', async () => {
    render(<SUT />);
    await userEvent.keyboard('{Meta>}s{/Meta}');
    await waitFor(() => expect(OnSaveViewAction).toHaveBeenCalledTimes(1));
  });

  describe('with an immutable scope', () => {
    const immutableView = mockView.toBuilder().scope('ILLUMINATE').build();

    beforeEach(() => {
      asMock(OnSaveViewAction).mockClear();
      asMock(useScopePermissions).mockReturnValue({
        loadingScopePermissions: false,
        scopePermissions: { is_mutable: false, is_deletable: false },
        checkPermissions: () => false,
      });
    });

    it('disables saving and editing metadata', async () => {
      render(<SUT view={immutableView} />);

      expect(await screen.findByRole('button', { name: 'Save dashboard' })).toHaveAttribute('aria-disabled', 'true');

      await userEvent.click(await screen.findByRole('button', { name: /more actions/i }));

      expect(await screen.findByRole('menuitem', { name: /edit metadata/i })).toBeDisabled();
    });

    it('sends only Illuminate clone telemetry with the original title when saving as a new dashboard', async () => {
      render(<SUT view={immutableView} />);

      await openDashboardSaveForm();
      await submitDashboardSaveForm();

      await waitFor(() =>
        expect(sendTelemetry).toHaveBeenCalledWith(TELEMETRY_EVENT_TYPE.DASHBOARD_ACTION.ILLUMINATE_DASHBOARD_CLONED, {
          app_pathname: 'dashboard',
          app_action_value: 'illuminate-dashboard-clone',
          event_details: { dashboard_title: 'View title' },
        }),
      );
      expect(sendTelemetry).not.toHaveBeenCalledWith(
        TELEMETRY_EVENT_TYPE.DASHBOARD_ACTION.DASHBOARD_NEW_SAVED,
        expect.anything(),
      );
    });

    it('still allows saving as a new dashboard', async () => {
      render(<SUT view={immutableView} />);

      expect(await screen.findByTitle(/Save as new dashboard/)).toBeEnabled();
    });

    it('explains why saving is disabled', async () => {
      render(<SUT view={immutableView} />);

      await userEvent.hover(await screen.findByRole('button', { name: 'Save dashboard' }));

      expect(await screen.findByText(/This dashboard is read-only/i)).toBeInTheDocument();
    });

    it('does not save view when clicking the disabled save button', async () => {
      render(<SUT view={immutableView} />);

      await userEvent.click(await screen.findByRole('button', { name: 'Save dashboard' }));

      expect(OnSaveViewAction).not.toHaveBeenCalled();
    });

    it('does not save view when pressing related keyboard shortcut', async () => {
      render(<SUT view={immutableView} />);

      await screen.findByRole('button', { name: 'Save dashboard' });
      await userEvent.keyboard('{Meta>}s{/Meta}');

      expect(OnSaveViewAction).not.toHaveBeenCalled();
    });
  });
});

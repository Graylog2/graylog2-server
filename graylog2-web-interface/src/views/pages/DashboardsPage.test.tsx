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
import { render, screen, waitFor } from 'wrappedTestingLibrary';
import moment from 'moment';
import userEvent from '@testing-library/user-event';
import { PluginStore } from 'graylog-web-plugin/plugin';

import type { DashboardsStoreState } from 'views/stores/DashboardsStore';
import { asMock } from 'helpers/mocking';
import Routes from 'routing/Routes';
import useDashboards from 'views/logic/dashboards/useDashboards';
import { simpleView } from 'views/test/ViewFixtures';
import UserDateTimeContext from 'contexts/UserDateTimeContext';
import { ViewManagementActions } from 'views/stores/ViewManagementStore';

import DashboardsPage from './DashboardsPage';

jest.mock('routing/Routes', () => ({
  pluginRoute: jest.fn(),
}));

jest.mock('views/logic/dashboards/useDashboards');

jest.mock('views/stores/ViewManagementStore', () => ({
  ViewManagementActions: {
    delete: jest.fn(),
  },
}));

const SUT = () => (
  <UserDateTimeContext.Provider value={{
    formatTime: (dateTime) => dateTime.toString(),
    toUserTimezone: (time) => moment(time),
    userTimezone: 'Europe/Paris',
  }}>
    <DashboardsPage />
  </UserDateTimeContext.Provider>
);

const noDashboards: DashboardsStoreState = {
  list: [],
  pagination: { count: 0, page: 1, perPage: 10, total: 0 },
};

const simpleDashboardList: DashboardsStoreState = {
  list: [simpleView()],
  pagination: { count: 1, page: 1, perPage: 10, total: 1 },
};

const mockDashboards = (dashboardState: DashboardsStoreState) => {
  asMock(useDashboards).mockReturnValue(dashboardState);
};

const clickDashboardAction = async (dashboardId: string, action: string) => {
  const actionsButton = (await screen.findAllByTestId(`dashboard-actions-dropdown-${dashboardId}`))[0];
  userEvent.click(actionsButton);

  userEvent.click((await screen.findAllByRole('menuitem', { name: action }))[0]);
};

describe('DashboardsPage', () => {
  let oldWindowConfirm;

  beforeEach(() => {
    oldWindowConfirm = window.confirm;
    window.confirm = jest.fn();

    asMock(Routes.pluginRoute).mockImplementation((key: string) => {
      switch (key) {
        case 'DASHBOARDS_NEW':
          return '/dashboards/new';
        case 'DASHBOARDS_VIEWID':
          return (id: string) => `/dashboards/${id}`;
        default:
          throw Error(`Invalid route: ${key}`);
      }
    });
  });

  afterEach(() => {
    window.confirm = oldWindowConfirm;
  });

  it('shows placeholder text if dashboards are empty', async () => {
    mockDashboards(noDashboards);

    render(<SUT />);

    await screen.findByText(/Create a new dashboard here/);
  });

  it('shows list of dashboards', async () => {
    mockDashboards(simpleDashboardList);
    render(<SUT />);

    await screen.findByRole('link', { name: 'Foo' });
  });

  it('does not delete dashboard when user clicks cancel', async () => {
    asMock(window.confirm).mockReturnValue(false);

    render(<SUT />);

    await clickDashboardAction('foo', 'Delete');

    await waitFor(() => expect(window.confirm).toHaveBeenCalledWith('Are you sure you want to delete "Foo"?'));

    expect(ViewManagementActions.delete).not.toHaveBeenCalledWith(expect.objectContaining({ id: 'foo' }));
  });

  it('deletes dashboard when user confirms deletion', async () => {
    asMock(window.confirm).mockReturnValue(true);

    render(<SUT />);

    await clickDashboardAction('foo', 'Delete');

    await waitFor(() => expect(window.confirm).toHaveBeenCalledWith('Are you sure you want to delete "Foo"?'));

    expect(ViewManagementActions.delete).toHaveBeenCalledWith(expect.objectContaining({ id: 'foo' }));
  });

  describe('supports dashboard deletion hook', () => {
    const deletingDashboard = jest.fn(() => Promise.resolve(true));

    const plugin = {
      exports: {
        'views.hooks.confirmDeletingDashboard': [deletingDashboard],
      },
    };

    beforeEach(() => {
      PluginStore.register(plugin);
      asMock(ViewManagementActions.delete).mockClear();
      asMock(ViewManagementActions.delete).mockImplementation((view) => Promise.resolve(view));
      mockDashboards(simpleDashboardList);
    });

    afterEach(() => {
      PluginStore.unregister(plugin);
    });

    it('triggers hook when deleting dashboard', async () => {
      render(<SUT />);

      await clickDashboardAction('foo', 'Delete');

      await waitFor(() => expect(ViewManagementActions.delete).toHaveBeenCalledWith(expect.objectContaining({ id: 'foo' })));

      expect(deletingDashboard).toHaveBeenCalledWith(simpleDashboardList.list[0]);
    });

    it('does not delete dashboard when hook returns false', async () => {
      asMock(deletingDashboard).mockResolvedValue(false);

      render(<SUT />);

      await clickDashboardAction('foo', 'Delete');

      await waitFor(() => expect(deletingDashboard).toHaveBeenCalledWith(simpleDashboardList.list[0]));

      expect(ViewManagementActions.delete).not.toHaveBeenCalledWith(expect.objectContaining({ id: 'foo' }));
    });

    it('resorts to default behavior when hook returns `null`', async () => {
      asMock(deletingDashboard).mockReturnValue(null);
      asMock(window.confirm).mockReturnValue(true);

      render(<SUT />);

      await clickDashboardAction('foo', 'Delete');

      await waitFor(() => expect(deletingDashboard).toHaveBeenCalledWith(simpleDashboardList.list[0]));

      expect(window.confirm).toHaveBeenCalledWith('Are you sure you want to delete "Foo"?');

      expect(ViewManagementActions.delete).toHaveBeenCalledWith(expect.objectContaining({ id: 'foo' }));
    });

    it('resorts to default behavior when hook throws error', async () => {
      const error = Error('Boom!');
      asMock(deletingDashboard).mockImplementation(() => { throw error; });
      asMock(window.confirm).mockReturnValue(true);

      render(<SUT />);

      /* eslint-disable no-console */
      const oldConsoleTrace = console.trace;
      console.trace = jest.fn();

      await clickDashboardAction('foo', 'Delete');

      await waitFor(() => expect(console.trace).toHaveBeenCalledWith('Exception occurred in deletion confirmation hook: ', error));
      console.trace = oldConsoleTrace;
      /* eslint-enable no-console */

      await waitFor(() => expect(deletingDashboard).toHaveBeenCalledWith(simpleDashboardList.list[0]));

      expect(window.confirm).toHaveBeenCalledWith('Are you sure you want to delete "Foo"?');

      expect(ViewManagementActions.delete).toHaveBeenCalledWith(expect.objectContaining({ id: 'foo' }));
    });
  });
});

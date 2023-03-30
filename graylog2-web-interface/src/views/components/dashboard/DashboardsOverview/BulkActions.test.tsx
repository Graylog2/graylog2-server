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
import userEvent from '@testing-library/user-event';

import fetch from 'logic/rest/FetchProvider';
import UserNotification from 'util/UserNotification';
import { asMock } from 'helpers/mocking';

import BulkActions from './BulkActions';

jest.mock('logic/rest/FetchProvider', () => jest.fn());

jest.mock('util/UserNotification', () => ({
  error: jest.fn(),
  success: jest.fn(),
}));

describe('DashboardsOverview BulkActionsRow', () => {
  const openActionsDropdown = async () => {
    await screen.findByRole('button', {
      name: /bulk actions/i,
    });
  };

  const deleteDashboards = async () => {
    userEvent.click(await screen.findByRole('menuitem', { name: /delete/i }));
  };

  beforeEach(() => {
    window.confirm = jest.fn(() => true);
  });

  it('should delete selected dashboards', async () => {
    asMock(fetch).mockReturnValue(Promise.resolve({ failures: [] }));
    const setSelectedDashboardIds = jest.fn();

    render(<BulkActions selectedDashboardIds={['dashboard-id-1', 'dashboard-id-2']}
                        setSelectedDashboardIds={setSelectedDashboardIds} />);

    await openActionsDropdown();
    await deleteDashboards();

    expect(window.confirm).toHaveBeenCalledWith('Do you really want to remove 2 dashboards?');

    await waitFor(() => expect(fetch).toHaveBeenCalledWith(
      'POST',
      expect.stringContaining('/views/bulk_delete'),
      { entity_ids: ['dashboard-id-1', 'dashboard-id-2'] },
    ));

    expect(UserNotification.success).toHaveBeenCalledWith('2 dashboards were deleted successfully.', 'Success');
    expect(setSelectedDashboardIds).toHaveBeenCalledWith([]);
  });

  it('should display warning and not reset dashboards which could not be deleted', async () => {
    asMock(fetch).mockReturnValue(Promise.resolve({
      failures: [
        { entity_id: 'dashboard-id-1', failure_explanation: 'The dashboard cannot be deleted.' },
      ],
    }));

    const setSelectedDashboardIds = jest.fn();

    render(<BulkActions selectedDashboardIds={['dashboard-id-1', 'dashboard-id-2']}
                        setSelectedDashboardIds={setSelectedDashboardIds} />);

    await openActionsDropdown();
    await deleteDashboards();

    expect(window.confirm).toHaveBeenCalledWith('Do you really want to remove 2 dashboards?');

    await waitFor(() => expect(fetch).toHaveBeenCalledWith(
      'POST',
      expect.stringContaining('/views/bulk_delete'),
      { entity_ids: ['dashboard-id-1', 'dashboard-id-2'] },
    ));

    expect(UserNotification.error).toHaveBeenCalledWith('1 out of 2 selected dashboards could not be deleted.');
    expect(setSelectedDashboardIds).toHaveBeenCalledWith(['dashboard-id-1']);
  });
});

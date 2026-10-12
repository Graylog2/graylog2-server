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

import { Views } from '@graylog/server-api';

import UserNotification from 'util/UserNotification';
import { asMock } from 'helpers/mocking';
import useSelectedEntities from 'components/common/EntityDataTable/hooks/useSelectedEntities';
import useWindowConfirmMock from 'helpers/mocking/useWindowConfirmMock';

import BulkActions from './BulkActions';

jest.mock('@graylog/server-api', () => ({ Views: { bulkDelete: jest.fn() } }));
jest.mock('components/common/EntityDataTable/hooks/useSelectedEntities');

jest.mock('util/UserNotification', () => ({
  error: jest.fn(),
  success: jest.fn(),
}));

describe('DashboardsOverview BulkActionsRow', () => {
  const useSelectedEntitiesResponse = {
    selectedEntities: [],
    setSelectedEntities: () => {},
    selectEntity: () => {},
    deselectEntity: () => {},
    toggleEntitySelect: () => {},
    isSomeRowsSelected: false,
    isAllRowsSelected: false,
  };

  const openActionsDropdown = async () => {
    await userEvent.click(
      await screen.findByRole('button', {
        name: /bulk actions/i,
      }),
    );

    await screen.findByRole('menu');
  };

  const deleteDashboards = async () => {
    await userEvent.click(await screen.findByRole('menuitem', { name: /delete/i }));
    await waitFor(() => expect(screen.queryByRole('menu')).not.toBeInTheDocument());
  };

  useWindowConfirmMock();

  beforeEach(() => {
    asMock(useSelectedEntities).mockReturnValue(useSelectedEntitiesResponse);
  });

  it('should delete selected dashboards', async () => {
    asMock(Views.bulkDelete).mockReturnValue(Promise.resolve({ failures: [], successfully_performed: 2, errors: [] }));
    const setSelectedEntities = jest.fn();

    asMock(useSelectedEntities).mockReturnValue({
      ...useSelectedEntitiesResponse,
      selectedEntities: ['dashboard-id-1', 'dashboard-id-2'],
      setSelectedEntities,
      isAllRowsSelected: true,
    });

    render(<BulkActions />);

    await openActionsDropdown();
    await deleteDashboards();

    expect(window.confirm).toHaveBeenCalledWith('Do you really want to remove 2 dashboards?');

    await waitFor(() =>
      expect(Views.bulkDelete).toHaveBeenCalledWith({
        entity_ids: ['dashboard-id-1', 'dashboard-id-2'],
      }),
    );

    expect(UserNotification.success).toHaveBeenCalledWith('2 dashboards were deleted successfully.', 'Success');
    expect(setSelectedEntities).toHaveBeenCalledWith([]);
  });

  it('should display warning and not reset dashboards which could not be deleted', async () => {
    asMock(Views.bulkDelete).mockReturnValue(
      Promise.resolve({
        failures: [{ entity_id: 'dashboard-id-1', failure_explanation: 'The dashboard cannot be deleted.' }],
        successfully_performed: 1,
        errors: [],
      }),
    );

    const setSelectedEntities = jest.fn();

    asMock(useSelectedEntities).mockReturnValue({
      ...useSelectedEntitiesResponse,
      selectedEntities: ['dashboard-id-1', 'dashboard-id-2'],
      setSelectedEntities,
      isAllRowsSelected: true,
    });

    render(<BulkActions />);

    await openActionsDropdown();
    await deleteDashboards();

    expect(window.confirm).toHaveBeenCalledWith('Do you really want to remove 2 dashboards?');

    await waitFor(() =>
      expect(Views.bulkDelete).toHaveBeenCalledWith({
        entity_ids: ['dashboard-id-1', 'dashboard-id-2'],
      }),
    );

    expect(UserNotification.error).toHaveBeenCalledWith('1 out of 2 selected dashboards could not be deleted.');
    expect(setSelectedEntities).toHaveBeenCalledWith(['dashboard-id-1']);
  });
});

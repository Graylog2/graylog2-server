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
import { render, screen, within } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';

import { asMock } from 'helpers/mocking';
import useFetchEntities from 'components/common/PaginatedEntityTable/useFetchEntities';
import useUserLayoutPreferences from 'components/common/EntityDataTable/hooks/useUserLayoutPreferences';
import { layoutPreferences } from 'fixtures/entityListLayoutPreferences';
import useNotificationDismiss from 'components/notifications/hooks/useNotificationDismiss';
import useNotificationBulkDismiss from 'components/notifications/hooks/useNotificationBulkDismiss';
import useNotificationBody from 'components/notifications/hooks/useNotificationBody';
import type { NotificationType } from 'components/notifications/types';

import SystemNotificationsTable from './SystemNotificationsTable';

jest.mock('components/common/PaginatedEntityTable/useFetchEntities');
jest.mock('components/common/EntityDataTable/hooks/useUserLayoutPreferences');
jest.mock('components/notifications/hooks/useNotificationDismiss');
jest.mock('components/notifications/hooks/useNotificationBulkDismiss');
jest.mock('components/notifications/hooks/useNotificationBody');

const notif1: NotificationType = {
  id: 'notif-1',
  title: 'Disk full on node1',
  description: 'The data directory is almost full.',
  timestamp: '2024-01-15T10:00:00.000Z',
  severity: 'urgent',
  type: 'no_master',
  key: 'no_master',
  node_id: 'node-1',
  details: {},
};

const attributes = [
  { id: 'title', title: 'Title', sortable: true },
  { id: 'description', title: 'Description', sortable: false },
  { id: 'severity', title: 'Severity', sortable: true },
  { id: 'timestamp', title: 'Timestamp', sortable: true },
];

const mockFetchData = (list: NotificationType[] = [notif1]) => ({
  data: {
    list,
    pagination: { total: list.length },
    attributes,
  },
  refetch: jest.fn(),
  isInitialLoading: false,
});

const mockMutate = jest.fn();

describe('SystemNotificationsTable', () => {
  beforeEach(() => {
    asMock(useUserLayoutPreferences).mockReturnValue({
      data: {
        ...layoutPreferences,
        attributes: {
          title: { status: 'show' },
          description: { status: 'show' },
          severity: { status: 'show' },
          timestamp: { status: 'show' },
        },
      },
      isInitialLoading: false,
      refetch: jest.fn(),
    });

    asMock(useNotificationDismiss).mockReturnValue({
      mutate: mockMutate,
      isPending: false,
    } as any);
    asMock(useNotificationBulkDismiss).mockReturnValue({
      mutate: mockMutate,
    } as any);
    asMock(useNotificationBody).mockReturnValue({
      data: undefined,
      isLoading: true,
      isError: false,
    });
  });

  it('renders column headers for default-visible attributes', async () => {
    asMock(useFetchEntities).mockReturnValue(mockFetchData());

    render(<SystemNotificationsTable />);

    await screen.findByRole('columnheader', { name: /title/i });
    await screen.findByRole('columnheader', { name: /severity/i });
    await screen.findByRole('columnheader', { name: /timestamp/i });

    expect(screen.queryByRole('columnheader', { name: /status/i })).not.toBeInTheDocument();
    expect(screen.queryByRole('columnheader', { name: /last changed by/i })).not.toBeInTheDocument();
    expect(screen.queryByRole('columnheader', { name: /type/i })).not.toBeInTheDocument();
  });

  it('renders notification title and description', async () => {
    asMock(useFetchEntities).mockReturnValue(mockFetchData());

    render(<SystemNotificationsTable />);

    const row = await screen.findByTestId('table-row-notif-1');

    await within(row).findByText('Disk full on node1');
    await within(row).findByText(/The data directory is almost full/);
  });

  it('renders a "Dismiss" action button for each row', async () => {
    asMock(useFetchEntities).mockReturnValue(mockFetchData());

    render(<SystemNotificationsTable />);

    const row = await screen.findByTestId('table-row-notif-1');

    await within(row).findByRole('button', { name: /dismiss/i });
  });

  it('calls dismiss mutation when the Dismiss button is clicked', async () => {
    asMock(useFetchEntities).mockReturnValue(mockFetchData([notif1]));

    render(<SystemNotificationsTable />);

    const row = await screen.findByTestId('table-row-notif-1');
    const dismissButton = within(row).getByRole('button', { name: /dismiss/i });

    await userEvent.click(dismissButton);

    expect(mockMutate).toHaveBeenCalledWith({ id: 'notif-1' });
  });

  describe('bulk actions', () => {
    it('renders bulk actions dropdown with a Dismiss item and no read-state items', async () => {
      asMock(useFetchEntities).mockReturnValue(mockFetchData([notif1]));

      render(<SystemNotificationsTable />);

      await screen.findByTestId('table-row-notif-1');

      const selectAll = screen.getByRole('checkbox', { name: /select all/i });

      await userEvent.click(selectAll);

      const bulkDropdown = await screen.findByRole('button', {
        name: /bulk actions/i,
      });

      await userEvent.click(bulkDropdown);

      await screen.findByRole('menuitem', { name: /^dismiss$/i });
      expect(screen.queryByRole('menuitem', { name: /toggle read state/i })).not.toBeInTheDocument();
      expect(screen.queryByRole('menuitem', { name: /mark all as read/i })).not.toBeInTheDocument();
    });
  });

  describe('row expansion', () => {
    it('shows spinner while body is loading', async () => {
      asMock(useFetchEntities).mockReturnValue(mockFetchData([notif1]));
      asMock(useNotificationBody).mockReturnValue({
        data: undefined,
        isLoading: true,
        isError: false,
      });

      render(<SystemNotificationsTable />);

      const titleButton = await screen.findByRole('button', {
        name: /show full message for: disk full on node1/i,
      });

      await userEvent.click(titleButton);

      await screen.findByText('Loading...');
    });

    it('renders HTML body in expanded section', async () => {
      asMock(useFetchEntities).mockReturnValue(mockFetchData([notif1]));
      asMock(useNotificationBody).mockReturnValue({
        data: {
          title: 'Disk full on node1',
          description: '<p>Full body content here</p>',
        },
        isLoading: false,
        isError: false,
      });

      render(<SystemNotificationsTable />);

      const titleButton = await screen.findByRole('button', {
        name: /show full message for: disk full on node1/i,
      });

      await userEvent.click(titleButton);

      await screen.findByText(/full body content here/i);
    });

    it('shows fallback message when body fetch fails', async () => {
      asMock(useFetchEntities).mockReturnValue(mockFetchData([notif1]));
      asMock(useNotificationBody).mockReturnValue({
        data: undefined,
        isLoading: false,
        isError: true,
      });

      render(<SystemNotificationsTable />);

      const titleButton = await screen.findByRole('button', {
        name: /show full message for: disk full on node1/i,
      });

      await userEvent.click(titleButton);

      await screen.findByText(/could not load full notification body/i);
    });
  });

  describe('empty and error states', () => {
    it('shows empty state when there are no notifications', async () => {
      asMock(useFetchEntities).mockReturnValue({
        data: {
          list: [],
          pagination: { total: 0 },
          attributes,
        },
        refetch: jest.fn(),
        isInitialLoading: false,
      });

      render(<SystemNotificationsTable />);

      await screen.findByText(/no notifications have been found/i);
    });

    it('shows loading state while fetching', async () => {
      asMock(useFetchEntities).mockReturnValue({
        data: undefined,
        refetch: jest.fn(),
        isInitialLoading: true,
      });

      render(<SystemNotificationsTable />);

      await screen.findByText('Loading...');
    });
  });
});

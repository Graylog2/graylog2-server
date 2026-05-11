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
import useNotificationToggleRead from 'components/notifications/SystemNotificationsTable/hooks/useNotificationToggleRead';
import useNotificationBulkToggleRead from 'components/notifications/SystemNotificationsTable/hooks/useNotificationBulkToggleRead';
import useNotificationMarkAllRead from 'components/notifications/SystemNotificationsTable/hooks/useNotificationMarkAllRead';
import useNotificationBody from 'components/notifications/SystemNotificationsTable/hooks/useNotificationBody';
import type { NotificationType } from 'components/notifications/types';

import SystemNotificationsTable from '../SystemNotificationsTable/SystemNotificationsTable';

jest.mock('components/common/PaginatedEntityTable/useFetchEntities');
jest.mock('components/common/EntityDataTable/hooks/useUserLayoutPreferences');
jest.mock('components/notifications/SystemNotificationsTable/hooks/useNotificationToggleRead');
jest.mock('components/notifications/SystemNotificationsTable/hooks/useNotificationBulkToggleRead');
jest.mock('components/notifications/SystemNotificationsTable/hooks/useNotificationMarkAllRead');
jest.mock('components/notifications/SystemNotificationsTable/hooks/useNotificationBody');

const notif1: NotificationType = {
  id: 'notif-1',
  title: 'Disk full on node1',
  description: 'The data directory is almost full.',
  is_read: false,
  actor: { id: 'user-1', name: 'alice' },
  triggered_at: '2024-01-15T10:00:00.000Z',
  last_changed: '2024-01-15T10:00:00.000Z',
  severity: 'urgent',
  type: 'legacy_no_master',
  key: 'no_master',
  node_id: 'node-1',
  details: {},
};

const notif2: NotificationType = {
  id: 'notif-2',
  title: 'High journal utilization',
  description: 'Journal utilization is above 90%.',
  is_read: true,
  actor: null,
  triggered_at: '2024-01-14T08:00:00.000Z',
  last_changed: '2024-01-14T09:00:00.000Z',
  severity: 'normal',
  type: 'legacy_no_master',
  key: 'journal_utilization',
  node_id: 'node-2',
  details: {},
};

const attributes = [
  { id: 'title', title: 'Title', sortable: true },
  { id: 'description', title: 'Description', sortable: false },
  { id: 'is_read', title: 'Status', sortable: true },
  { id: 'actor.name', title: 'Actor', sortable: false },
  { id: 'triggered_at', title: 'Triggered at', sortable: true },
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
          is_read: { status: 'show' },
          'actor.name': { status: 'show' },
          triggered_at: { status: 'show' },
        },
      },
      isInitialLoading: false,
      refetch: jest.fn(),
    });

    asMock(useNotificationToggleRead).mockReturnValue({ mutate: mockMutate, isPending: false } as any);
    asMock(useNotificationBulkToggleRead).mockReturnValue({ mutate: mockMutate } as any);
    asMock(useNotificationMarkAllRead).mockReturnValue({ mutate: mockMutate } as any);
    asMock(useNotificationBody).mockReturnValue({ data: undefined, isLoading: true, isError: false });
  });

  it('renders column headers for default-visible attributes', async () => {
    asMock(useFetchEntities).mockReturnValue(mockFetchData());

    render(<SystemNotificationsTable />);

    await screen.findByRole('columnheader', { name: /title/i });
    await screen.findByRole('columnheader', { name: /description/i });
    await screen.findByRole('columnheader', { name: /status/i });
    await screen.findByRole('columnheader', { name: /actor/i });
    await screen.findByRole('columnheader', { name: /triggered at/i });
  });

  it('renders notification title, description, and actor', async () => {
    asMock(useFetchEntities).mockReturnValue(mockFetchData());

    render(<SystemNotificationsTable />);

    const row = await screen.findByTestId('table-row-notif-1');

    await within(row).findByText('Disk full on node1');
    await within(row).findByText(/The data directory is almost full/);
    await within(row).findByText('alice');
  });

  it('renders "System" for notifications without an actor', async () => {
    asMock(useFetchEntities).mockReturnValue(mockFetchData([notif2]));

    render(<SystemNotificationsTable />);

    const row = await screen.findByTestId('table-row-notif-2');

    await within(row).findByText('System');
  });

  it('shows "Mark as read" label in status cell for unread notification', async () => {
    asMock(useFetchEntities).mockReturnValue(mockFetchData([notif1]));

    render(<SystemNotificationsTable />);

    const row = await screen.findByTestId('table-row-notif-1');
    const statusButtons = within(row).getAllByRole('button', { name: /mark as read/i });

    expect(statusButtons.length).toBeGreaterThan(0);
  });

  it('shows "Mark as unread" label in status cell for read notification', async () => {
    asMock(useFetchEntities).mockReturnValue(mockFetchData([notif2]));

    render(<SystemNotificationsTable />);

    const row = await screen.findByTestId('table-row-notif-2');
    const statusButtons = within(row).getAllByRole('button', { name: /mark as unread/i });

    expect(statusButtons.length).toBeGreaterThan(0);
  });

  it('calls toggleRead mutation when status cell button is clicked', async () => {
    asMock(useFetchEntities).mockReturnValue(mockFetchData([notif1]));

    render(<SystemNotificationsTable />);

    const row = await screen.findByTestId('table-row-notif-1');
    const [firstToggle] = within(row).getAllByRole('button', { name: /mark as read/i });

    await userEvent.click(firstToggle);

    expect(mockMutate).toHaveBeenCalledWith({ id: 'notif-1', currentIsRead: false });
  });

  it('actions cell does not contain a delete button', async () => {
    asMock(useFetchEntities).mockReturnValue(mockFetchData([notif1]));

    render(<SystemNotificationsTable />);

    await screen.findByTestId('table-row-notif-1');

    expect(screen.queryByRole('button', { name: /delete/i })).not.toBeInTheDocument();
  });

  describe('bulk actions', () => {
    it('renders bulk actions dropdown with toggle and mark-all items', async () => {
      asMock(useFetchEntities).mockReturnValue(mockFetchData([notif1]));

      render(<SystemNotificationsTable />);

      await screen.findByTestId('table-row-notif-1');

      const selectAll = screen.getByRole('checkbox', { name: /select all/i });

      await userEvent.click(selectAll);

      const bulkDropdown = await screen.findByRole('button', { name: /bulk actions/i });

      await userEvent.click(bulkDropdown);

      await screen.findByRole('menuitem', { name: /toggle read state/i });
      await screen.findByRole('menuitem', { name: /mark all as read/i });
    });

    it('opens confirmation modal before marking all as read', async () => {
      asMock(useFetchEntities).mockReturnValue(mockFetchData([notif1]));

      render(<SystemNotificationsTable />);

      await screen.findByTestId('table-row-notif-1');

      const selectAll = screen.getByRole('checkbox', { name: /select all/i });

      await userEvent.click(selectAll);

      const bulkDropdown = await screen.findByRole('button', { name: /bulk actions/i });

      await userEvent.click(bulkDropdown);

      const markAllItem = await screen.findByRole('menuitem', { name: /mark all as read/i });

      await userEvent.click(markAllItem);

      await screen.findByText(/mark all notifications as read/i);
      await screen.findByText(/all notifications you have permission to view/i);
    });

    it('does not call markAllRead when cancel is clicked in confirmation modal', async () => {
      asMock(useFetchEntities).mockReturnValue(mockFetchData([notif1]));

      render(<SystemNotificationsTable />);

      await screen.findByTestId('table-row-notif-1');

      const selectAll = screen.getByRole('checkbox', { name: /select all/i });

      await userEvent.click(selectAll);

      const bulkDropdown = await screen.findByRole('button', { name: /bulk actions/i });

      await userEvent.click(bulkDropdown);

      await userEvent.click(await screen.findByRole('menuitem', { name: /mark all as read/i }));

      await screen.findByText(/mark all notifications as read/i);

      const markAllMutate = jest.fn();

      asMock(useNotificationMarkAllRead).mockReturnValue({ mutate: markAllMutate } as any);

      await userEvent.click(screen.getByRole('button', { name: /cancel/i }));

      expect(markAllMutate).not.toHaveBeenCalled();
      expect(screen.queryByText(/mark all notifications as read/i)).not.toBeInTheDocument();
    });
  });

  describe('row expansion', () => {
    it('shows spinner while body is loading', async () => {
      asMock(useFetchEntities).mockReturnValue(mockFetchData([notif1]));
      asMock(useNotificationBody).mockReturnValue({ data: undefined, isLoading: true, isError: false });

      render(<SystemNotificationsTable />);

      const titleButton = await screen.findByRole('button', { name: /show full message for: disk full on node1/i });

      await userEvent.click(titleButton);

      await screen.findByText('Loading...');
    });

    it('renders HTML body in expanded section', async () => {
      asMock(useFetchEntities).mockReturnValue(mockFetchData([notif1]));
      asMock(useNotificationBody).mockReturnValue({
        data: { title: 'Disk full on node1', description: '<p>Full body content here</p>' },
        isLoading: false,
        isError: false,
      });

      render(<SystemNotificationsTable />);

      const titleButton = await screen.findByRole('button', { name: /show full message for: disk full on node1/i });

      await userEvent.click(titleButton);

      await screen.findByText(/full body content here/i);
    });

    it('shows fallback message when body fetch fails', async () => {
      asMock(useFetchEntities).mockReturnValue(mockFetchData([notif1]));
      asMock(useNotificationBody).mockReturnValue({ data: undefined, isLoading: false, isError: true });

      render(<SystemNotificationsTable />);

      const titleButton = await screen.findByRole('button', { name: /show full message for: disk full on node1/i });

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

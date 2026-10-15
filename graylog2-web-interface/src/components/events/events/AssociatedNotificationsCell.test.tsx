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
import { render, screen } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';

import asMock from 'helpers/mocking/AsMock';
import usePermissions from 'hooks/usePermissions';
import useEventDefinitionBatched from 'components/event-definitions/hooks/useEventDefinitionBatched';
import useNotificationsByIds from 'components/event-notifications/hooks/useNotificationsByIds';

import AssociatedNotificationsCell from './AssociatedNotificationsCell';

jest.mock('hooks/usePermissions');
jest.mock('components/event-definitions/hooks/useEventDefinitionBatched');
jest.mock('components/event-notifications/hooks/useNotificationsByIds');

const permitAll = () => asMock(usePermissions).mockReturnValue({ isPermitted: () => true, isAnyPermitted: () => true });

const mockEventDefinition = (notificationIds: Array<string>, isLoading = false) =>
  asMock(useEventDefinitionBatched).mockReturnValue({
    data: isLoading ? undefined : { notifications: notificationIds.map((notification_id) => ({ notification_id })) },
    isLoading,
  } as unknown as ReturnType<typeof useEventDefinitionBatched>);

const mockNotifications = (
  notifications: Array<{ id: string; title: string }>,
  notPermittedIds: Array<string> = [],
  isLoading = false,
) => asMock(useNotificationsByIds).mockReturnValue({ data: notifications, notPermittedIds, isLoading });

describe('AssociatedNotificationsCell', () => {
  beforeEach(() => {
    permitAll();
  });

  it('shows the first notification by title and expands to the rest, sorted', async () => {
    mockEventDefinition(['n1', 'n2', 'n3']);
    mockNotifications([
      { id: 'n1', title: 'Pager' },
      { id: 'n2', title: 'Email' },
      { id: 'n3', title: 'Chat' },
    ]);

    render(<AssociatedNotificationsCell eventDefinitionId="def-1" />);

    expect(screen.getByText('Chat')).toBeInTheDocument();
    expect(screen.queryByText('Email')).not.toBeInTheDocument();
    expect(screen.getByText('+ 2')).toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: /show all/i }));

    const titles = ['Chat', 'Email', 'Pager'].map((title) => screen.getByText(title));

    expect(titles[0].compareDocumentPosition(titles[1]) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
    expect(titles[1].compareDocumentPosition(titles[2]) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
  });

  it('lists notifications the user cannot read as plain IDs after the readable ones', async () => {
    mockEventDefinition(['n1', 'n2']);
    mockNotifications([{ id: 'n1', title: 'Pager' }], ['n2']);

    render(<AssociatedNotificationsCell eventDefinitionId="def-1" />);

    await userEvent.click(screen.getByRole('button', { name: /show all/i }));

    expect(screen.getByText('Pager')).toBeInTheDocument();
    expect(screen.getByText('n2')).toBeInTheDocument();
  });

  it('renders nothing when the user cannot read the event definition', () => {
    asMock(usePermissions).mockReturnValue({ isPermitted: () => false, isAnyPermitted: () => false });
    mockEventDefinition(['n1']);
    mockNotifications([{ id: 'n1', title: 'Pager' }]);

    render(<AssociatedNotificationsCell eventDefinitionId="def-1" />);

    expect(screen.queryByText('Pager')).not.toBeInTheDocument();
    expect(useEventDefinitionBatched).toHaveBeenCalledWith(undefined);
  });

  it('renders nothing for a definition without notifications', () => {
    mockEventDefinition([]);
    mockNotifications([]);

    render(<AssociatedNotificationsCell eventDefinitionId="def-1" />);

    expect(screen.queryByRole('link')).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /show all/i })).not.toBeInTheDocument();
  });

  it('shows a spinner while the notification titles load', async () => {
    mockEventDefinition(['n1']);
    mockNotifications([], [], true);

    render(<AssociatedNotificationsCell eventDefinitionId="def-1" />);

    await screen.findByText(/loading/i);
  });
});

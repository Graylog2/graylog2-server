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

import asMock from 'helpers/mocking/AsMock';
import useNotifications from 'components/notifications/useNotifications';

import NotificationsList from './NotificationsList';

jest.mock('components/notifications/useNotifications');

describe('<NotificationsList>', () => {
  it('shows a loading indicator while notifications are being fetched', async () => {
    asMock(useNotifications).mockReturnValue({ data: undefined, isLoading: true });

    render(<NotificationsList />);

    await screen.findByText(/loading/i);
  });

  it('shows a loading indicator when fetching notifications failed', async () => {
    asMock(useNotifications).mockReturnValue({ data: undefined, isLoading: false });

    render(<NotificationsList />);

    await screen.findByText(/loading/i);
  });

  it('shows that there are no notifications', async () => {
    asMock(useNotifications).mockReturnValue({ data: { total: 0, notifications: [] }, isLoading: false });

    render(<NotificationsList />);

    await screen.findByRole('heading', { name: /no notifications/i });
  });
});

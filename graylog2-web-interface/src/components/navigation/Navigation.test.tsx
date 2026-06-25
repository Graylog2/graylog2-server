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
import type { Location } from 'history';
import { defaultUser } from 'defaultMockValues';

import mockComponent from 'helpers/mocking/MockComponent';
import { asMock } from 'helpers/mocking';
import Navigation from 'components/navigation/Navigation';
import useCurrentUser from 'hooks/useCurrentUser';
import useLocation from 'routing/useLocation';
import HotkeysProvider from 'contexts/HotkeysProvider';
import useNotificationBadgeCount from 'components/notifications/hooks/useNotificationBadgeCount';

jest.mock('./ScratchpadToggle', () => mockComponent('ScratchpadToggle'));
jest.mock('hooks/useCurrentUser');
jest.mock('routing/useLocation', () => jest.fn(() => ({ pathname: '' })));
jest.mock('@graylog/server-api', () => ({
  SystemNotifications: {
    listNotifications: jest.fn(async () => ({ total: 0 })),
    getPaginated: jest.fn(async () => ({ pagination: { total: 0 }, elements: [] })),
  },
}));
jest.mock('components/notifications/hooks/useNotificationBadgeCount');

describe('Navigation', () => {
  const SUT = () => (
    <HotkeysProvider>
      <Navigation />
    </HotkeysProvider>
  );

  beforeEach(() => {
    asMock(useCurrentUser).mockReturnValue(defaultUser);
    asMock(useLocation).mockReturnValue({ pathname: '/' } as Location);

    asMock(useNotificationBadgeCount).mockReturnValue({
      data: 1,
      isLoading: false,
    });
  });

  it('has common elements', async () => {
    render(<SUT />);

    await screen.findByRole('link', { name: /throughput/i });
    await screen.findByRole('button', { name: /help/i });
    await screen.findByRole('button', { name: /user menu for administrator/i });
  });

  it('shows notification badge when there are notifications', async () => {
    render(<SUT />);

    await screen.findByTestId('notification-badge');
  });

  it('does not show notification badge when there are no notifications', async () => {
    asMock(useNotificationBadgeCount).mockReturnValue({
      data: 0,
      isLoading: false,
    });

    render(<SUT />);

    await screen.findByRole('button', { name: /help/i });

    expect(screen.queryByTestId('notification-badge')).not.toBeInTheDocument();
  });
});

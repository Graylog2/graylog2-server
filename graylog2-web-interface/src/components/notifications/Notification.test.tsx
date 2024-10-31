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
import { render, act, screen, waitFor } from 'wrappedTestingLibrary';

import MockStore from 'helpers/mocking/StoreMock';
import asMock from 'helpers/mocking/AsMock';
import NotificationsFactory from 'logic/notifications/NotificationsFactory';
import { NotificationsActions, NotificationsStore } from 'stores/notifications/NotificationsStore';

import Notification from './Notification';

const notificationMessageFixture = {
  title: 'There is a node without any running inputs\n\n',
  description: '\n<span>\nThere is a node without any running inputs. This means that you are not receiving any messages from this\nnode at this point in time. This is most probably an indication of an error or misconfiguration.\n         You can click <a href="/system/inputs" target="_blank" rel="noreferrer">here</a> to solve this.\n</span>\n',
};
const notificationFixture = {
  severity: 'urgent',
  type: 'no_input_running',
  key: 'test',
  timestamp: '2022-12-12T10:55:55.014Z',
  node_id: '3fcc3889-18a3-4a0d-821c-0fd560d152e7',
};

jest.mock('stores/notifications/NotificationsStore', () => ({
  NotificationsStore: MockStore(
    ['getInitialState', jest.fn(() => ({
      messages: {},
    }))],
  ),
  NotificationsActions: {
    getInitialState: jest.fn(),
    getHtmlMessage: jest.fn(() => Promise.resolve(notificationMessageFixture)),
  },
}
));

describe('<Notification>', () => {
  afterEach(() => {
    jest.clearAllMocks();
    jest.useRealTimers();
  });

  test('should render Notification', async () => {
    render(<Notification notification={notificationFixture} />);

    await screen.findByText(/loading\.\.\./i);

    await waitFor(() => expect(NotificationsActions.getHtmlMessage).toHaveBeenCalledWith(notificationFixture.type, notificationFixture.key, NotificationsFactory.getValuesForNotification(notificationFixture)));
  });

  test('should render notification message', async () => {
    jest.useFakeTimers();

    asMock(NotificationsStore.getInitialState).mockReturnValue({
      messages: { [`${notificationFixture.type}-${notificationFixture.key}`]: notificationMessageFixture },
      total: 1,
      notifications: [notificationFixture],
    });

    render(<Notification notification={notificationFixture} />);

    act(() => {
      jest.advanceTimersByTime(100);
    });

    const alert = screen.getByRole('alert', {
      name: /there is a node without any running inputs/i,
    });

    await waitFor(() => expect(alert).toBeInTheDocument());
  });
});

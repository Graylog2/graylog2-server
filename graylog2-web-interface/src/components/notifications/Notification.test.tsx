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
import useNotificationMessage from 'components/notifications/useNotificationMessage';

import Notification from './Notification';

const notificationMessageFixture = {
  title: 'There is a node without any running inputs\n\n',
  description:
    '\n<span>\nThere is a node without any running inputs. This means that you are not receiving any messages from this\nnode at this point in time. This is most probably an indication of an error or misconfiguration.\n         You can click <a href="/system/inputs" target="_blank" rel="noreferrer">here</a> to solve this.\n</span>\n',
};
const notificationFixture = {
  id: 'deadbeef',
  details: {},
  validations: {},
  fields: {},
  severity: 'urgent',
  type: 'no_input_running',
  key: 'test',
  timestamp: '2022-12-12T10:55:55.014Z',
  node_id: '3fcc3889-18a3-4a0d-821c-0fd560d152e7',
} as const;

jest.mock('components/notifications/useNotificationMessage');

describe('<Notification>', () => {
  beforeEach(() => {
    asMock(useNotificationMessage).mockReturnValue(notificationMessageFixture);
  });

  test('it render notification message', async () => {
    render(<Notification notification={notificationFixture} />);

    await screen.findByRole('alert', {
      name: /there is a node without any running inputs/i,
    });
  });
});

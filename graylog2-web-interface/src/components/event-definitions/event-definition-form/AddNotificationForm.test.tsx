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

import selectEvent from 'helpers/selectEvent';
import useLocation from 'routing/useLocation';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { asMock } from 'helpers/mocking';
import AddNotificationForm from 'components/event-definitions/event-definition-form/AddNotificationForm';

jest.mock('routing/useLocation');
jest.mock('logic/telemetry/useSendTelemetry');
jest.mock('logic/telemetry/withTelemetry', () => <T,>(Component: React.FC<T>) => (props: T) => (
  <Component {...props} sendTelemetry={() => {}} />
));

describe('AddNotificationForm', () => {
  beforeEach(() => {
    asMock(useLocation).mockReturnValue({
      pathname: '/alerts/definitions/new',
      search: '',
      hash: '',
      state: null,
      key: 'mock-key',
    });
    asMock(useSendTelemetry).mockReturnValue(jest.fn());
  });

  const notifications = [{ id: 'notification-id-1', title: 'My Notification' }];

  it('does not assign a notification when none is selected', async () => {
    const onChange = jest.fn();

    render(<AddNotificationForm notifications={notifications} onChange={onChange} onCancel={() => {}} />);

    await userEvent.click(await screen.findByRole('button', { name: /add notification/i }));

    expect(onChange).not.toHaveBeenCalled();
  });

  it('assigns the selected notification', async () => {
    const onChange = jest.fn();

    render(<AddNotificationForm notifications={notifications} onChange={onChange} onCancel={() => {}} />);

    await selectEvent.chooseOption('Select Notification', 'My Notification');
    await userEvent.click(await screen.findByRole('button', { name: /add notification/i }));

    expect(onChange).toHaveBeenCalledWith('notification-id-1');
  });
});

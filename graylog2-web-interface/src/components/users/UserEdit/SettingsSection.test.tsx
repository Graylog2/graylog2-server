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
import { render, fireEvent, waitFor, screen, act } from 'wrappedTestingLibrary';
import { alice } from 'fixtures/users';
import selectEvent from 'react-select-event';

import SettingsSection from './SettingsSection';

const exampleUser = alice.toBuilder()
  .sessionTimeoutMs(36000000)
  .timezone('Europe/Berlin')
  .build();

describe('<SettingsSection />', () => {
  it('should use user settings as initial values', async () => {
    const onSubmitStub = jest.fn();
    render(<SettingsSection user={exampleUser} onSubmit={(data) => onSubmitStub(data)} />);

    const submitButton = screen.getByText('Update Settings');

    fireEvent.click(submitButton);

    await waitFor(() => expect(onSubmitStub).toHaveBeenCalledTimes(1));

    expect(onSubmitStub).toHaveBeenCalledWith({
      session_timeout_ms: exampleUser.sessionTimeoutMs,
      timezone: exampleUser.timezone,
    });
  });

  it('should allow session timeout name and timezone change', async () => {
    const onSubmitStub = jest.fn();
    render(<SettingsSection user={exampleUser} onSubmit={(data) => onSubmitStub(data)} />);

    const timeoutAmountInput = screen.getByPlaceholderText('Timeout amount');
    const timezoneSelect = screen.getByLabelText('Time Zone');
    const submitButton = screen.getByText('Update Settings');

    act(() => {
      fireEvent.change(timeoutAmountInput, { target: { value: '40' } });
      selectEvent.openMenu(timezoneSelect);
      selectEvent.select(timezoneSelect, 'Vancouver');
      fireEvent.click(submitButton);
    });

    await waitFor(() => expect(onSubmitStub).toHaveBeenCalledTimes(1));

    expect(onSubmitStub).toHaveBeenCalledWith({
      session_timeout_ms: 144000000,
      timezone: 'America/Vancouver',
    });
  });
});

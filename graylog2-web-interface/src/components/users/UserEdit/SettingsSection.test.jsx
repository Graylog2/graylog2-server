// @flow strict
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

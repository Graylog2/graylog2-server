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
import React from 'react';
import { render, screen, act, fireEvent, waitFor } from 'wrappedTestingLibrary';

import { HTTPHeaderAuthConfigActions } from 'stores/authentication/HTTPHeaderAuthConfigStore';
import HTTPHeaderAuthConfig from 'logic/authentication/HTTPHeaderAuthConfig';

import HTTPHeaderAuthConfigSection from './HTTPHeaderAuthConfigSection';

const mockHTTPHeaderAuthConfig = HTTPHeaderAuthConfig.builder()
  .usernameHeader('Remote-User')
  .enabled(true)
  .build();

jest.mock('stores/authentication/HTTPHeaderAuthConfigStore', () => ({
  HTTPHeaderAuthConfigActions: {
    load: jest.fn(() => Promise.resolve(mockHTTPHeaderAuthConfig)),
    update: jest.fn(() => Promise.resolve()),
  },
}));

jest.useFakeTimers();

describe('<HTTPHeaderAuthConfigSection />', () => {
  it('should display loading indicator while loading', async () => {
    render(<HTTPHeaderAuthConfigSection />);

    act(() => {
      jest.advanceTimersByTime(200);
    });

    await screen.findByText('Loading...');
  });

  it('should use current config as initial values', async () => {
    render(<HTTPHeaderAuthConfigSection />);

    const submitButton = await screen.findByText('Update Config');
    fireEvent.click(submitButton);

    await waitFor(() => expect(HTTPHeaderAuthConfigActions.update).toHaveBeenCalledTimes(1));

    expect(HTTPHeaderAuthConfigActions.update).toHaveBeenCalledWith({ username_header: 'Remote-User', enabled: true });
  });

  it('should update config', async () => {
    render(<HTTPHeaderAuthConfigSection />);

    const submitButton = await screen.findByText('Update Config');
    const enabledHeaderCheckbox = screen.getByLabelText('Enable single sign-on via HTTP header');
    const usernameHeaderInput = screen.getByLabelText('Username header');

    fireEvent.click(enabledHeaderCheckbox);
    fireEvent.change(usernameHeaderInput, { target: { value: 'New-Header' } });
    fireEvent.click(submitButton);

    await waitFor(() => expect(HTTPHeaderAuthConfigActions.update).toHaveBeenCalledTimes(1));

    expect(HTTPHeaderAuthConfigActions.update).toHaveBeenCalledWith({ username_header: 'New-Header', enabled: false });
  });
});

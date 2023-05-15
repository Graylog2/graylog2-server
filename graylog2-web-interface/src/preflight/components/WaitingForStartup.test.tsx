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
import { render, screen, waitFor } from 'wrappedTestingLibrary';

import useServerAvailability from 'preflight/hooks/useServerAvailability';
import { asMock } from 'helpers/mocking';

import WaitingForStartup from './WaitingForStartup';

jest.mock('preflight/hooks/useServerAvailability', () => jest.fn());

describe('WaitingForStartup', () => {
  let windowLocation;

  beforeAll(() => {
    window.confirm = jest.fn(() => true);

    Object.defineProperty(window, 'location', {
      configurable: true,
      value: { reload: jest.fn() },
    });
  });

  beforeEach(() => {
    asMock(useServerAvailability).mockReturnValue(({
      data: false,
    }));
  });

  afterAll(() => {
    Object.defineProperty(window, 'location', { configurable: true, value: windowLocation });
  });

  it('should not reload page while server is starting', async () => {
    render(<WaitingForStartup />);

    await screen.findByText(/The Graylog server is currently starting./);

    expect(window.location.reload).not.toHaveBeenCalled();
  });

  it('should reload page after server started', async () => {
    asMock(useServerAvailability).mockReturnValue(({
      data: true,
    }));

    render(<WaitingForStartup />);

    await screen.findByText(/The Graylog server is currently starting./);
    await waitFor(() => expect(window.location.reload).toHaveBeenCalledTimes(1));
  });
});

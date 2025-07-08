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
import { renderPreflight, screen, waitFor } from 'wrappedTestingLibrary';

import useServerAvailability from 'preflight/hooks/useServerAvailability';
import { asMock } from 'helpers/mocking';
import { fullPageReload } from 'util/URLUtils';

import WaitingForStartup from './WaitingForStartup';

jest.mock('preflight/hooks/useServerAvailability', () => jest.fn());

jest.mock('util/navigation', () => ({
  fullPageReload: jest.fn(),
}));

describe('WaitingForStartup', () => {
  beforeEach(() => {
    asMock(useServerAvailability).mockReturnValue({
      data: false,
    });
  });

  it('should not reload page while server is starting', async () => {
    renderPreflight(<WaitingForStartup />);

    await screen.findByText(/The Graylog server is currently starting./);

    expect(fullPageReload).not.toHaveBeenCalled();
  });

  it('should reload page after server started', async () => {
    asMock(useServerAvailability).mockReturnValue({
      data: true,
    });

    renderPreflight(<WaitingForStartup />);

    await screen.findByText(/The Graylog server is currently starting./);
    await waitFor(() => expect(fullPageReload).toHaveBeenCalledTimes(1));
  });
});

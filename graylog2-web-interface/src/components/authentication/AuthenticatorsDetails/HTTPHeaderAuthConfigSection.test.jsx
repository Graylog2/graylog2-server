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
import { render, screen, act } from 'wrappedTestingLibrary';

import HTTPHeaderAuthConfig from 'logic/authentication/HTTPHeaderAuthConfig';

import HTTPHeaderAuthConfigSection from './HTTPHeaderAuthConfigSection';

const mockHTTPHeaderAuthConfig = HTTPHeaderAuthConfig.builder()
  .usernameHeader('Remote-User')
  .enabled(true)
  .build();

jest.mock('stores/authentication/HTTPHeaderAuthConfigStore', () => ({
  HTTPHeaderAuthConfigActions: {
    load: jest.fn(() => Promise.resolve(mockHTTPHeaderAuthConfig)),
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

  it('should load and display HTTP header auth config details', async () => {
    render(<HTTPHeaderAuthConfigSection />);

    await screen.findByText('Enabled');

    expect(screen.getByText('yes')).toBeInTheDocument();
    expect(screen.getByText('Remote-User')).toBeInTheDocument();
  });
});

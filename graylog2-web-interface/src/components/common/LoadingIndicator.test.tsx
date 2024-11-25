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
import { render, screen } from 'wrappedTestingLibrary';
import { act } from 'react';

import LoadingIndicator from 'components/common/LoadingIndicator';

jest.useFakeTimers();

describe('<LoadingIndicator />', () => {
  it('Use defaults props to change loading text after default timeout', async () => {
    render(<LoadingIndicator />);

    await screen.findByText('Loading...');

    act(() => {
      jest.advanceTimersByTime(20000);
    });

    await screen.findByText('This is taking a bit longer, please hold on...');

    expect(screen.queryByText('Loading...')).not.toBeInTheDocument();
  });

  it('Use passed props to change loading text after timeout', async () => {
    render(<LoadingIndicator text="Loading data..." longWaitText="It takes some time. Please wait a bit more..." longWaitTimeout={30000} />);

    await screen.findByText('Loading data...');

    act(() => {
      jest.advanceTimersByTime(30000);
    });

    await screen.findByText('It takes some time. Please wait a bit more...');

    expect(screen.queryByText('Loading data...')).not.toBeInTheDocument();
  });
});

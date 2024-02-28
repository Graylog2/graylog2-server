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

import LoadingIndicator from 'components/common/LoadingIndicator';

describe('<LoadingIndicator />', () => {
  it('Use defaults props to change loading text after timeout', async () => {
    render(<LoadingIndicator longWaitTimeout={1000} />);

    await screen.findByText('Loading...');

    await waitFor(() => {
      expect(screen.queryByText('Loading...')).not.toBeInTheDocument();
    }, { timeout: 1000 });

    await waitFor(() => {
      expect(screen.getByText('This is taking a bit longer, please hold on...')).toBeInTheDocument();
    }, { timeout: 1000 });
  });

  it('Use passed props to change loading text after timeout', async () => {
    render(<LoadingIndicator text="Loading data..." longWaitText="It takes some time. Please wait a bit more..." longWaitTimeout={1000} />);

    await screen.findByText('Loading data...');

    await waitFor(() => {
      expect(screen.queryByText('Loading data...')).not.toBeInTheDocument();
    }, { timeout: 1000 });

    await waitFor(() => {
      expect(screen.getByText('It takes some time. Please wait a bit more...')).toBeInTheDocument();
    }, { timeout: 1000 });
  });
});

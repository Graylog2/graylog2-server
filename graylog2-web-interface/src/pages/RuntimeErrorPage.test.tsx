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
import { render, fireEvent, waitFor } from 'wrappedTestingLibrary';
import mockComponent from 'helpers/mocking/MockComponent';

import RuntimeErrorPage from './RuntimeErrorPage';

jest.mock('components/layout/Footer', () => mockComponent('Footer'));

describe('RuntimeErrorPage', () => {
  const SimpleRuntimeErrorPage = () => <RuntimeErrorPage error={new Error('The error message')} componentStack="The component stack" />;

  it('displays runtime error', () => {
    const { getByText } = render(<SimpleRuntimeErrorPage />);

    expect(getByText('Something went wrong.')).not.toBeNull();
    expect(getByText('The error message')).not.toBeNull();
  });

  it('displays component stack', async () => {
    const { getByText, queryByText } = render(<SimpleRuntimeErrorPage />);

    expect(getByText('Something went wrong.')).not.toBeNull();
    expect(queryByText('The component stack')).toBeNull();

    const showMoreButton = getByText('Show more');

    fireEvent.click(showMoreButton);

    waitFor(() => expect(getByText('The component stack')).not.toBeNull());
  });
});

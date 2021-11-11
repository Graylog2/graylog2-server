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
import { render, waitFor, screen } from 'wrappedTestingLibrary';

import { QueriesActions } from 'views/stores/QueriesStore';
import { asMock } from 'helpers/mocking';

import QueryValidation from './QueryValidation';

jest.mock('views/stores/QueriesStore', () => ({
  QueriesActions: {
    validateQueryString: jest.fn(() => Promise.resolve()),
  },
}));

describe('QueryValidation', () => {
  it('should validate query on mount', async () => {
    render(<QueryValidation query="initial query" />);

    await waitFor(() => expect(QueriesActions.validateQueryString).toHaveBeenCalledTimes(1));

    expect(QueriesActions.validateQueryString).toHaveBeenCalledWith('initial query');
  });

  it('should validate query on change', async () => {
    const { rerender } = render(<QueryValidation query="initial query" />);

    await waitFor(() => expect(QueriesActions.validateQueryString).toHaveBeenCalledTimes(1));

    rerender(<QueryValidation query="updated query" />);

    await waitFor(() => expect(QueriesActions.validateQueryString).toHaveBeenCalledTimes(2));

    expect(QueriesActions.validateQueryString).toHaveBeenCalledWith('updated query');
  });

  it('should display validation error icon', async () => {
    asMock(QueriesActions.validateQueryString).mockReturnValue(Promise.resolve({ status: 'ERROR', explanations: ['test'] }));
    render(<QueryValidation query="initial query" />);

    await screen.findByTitle('Toggle validation error explanation');
  });
});

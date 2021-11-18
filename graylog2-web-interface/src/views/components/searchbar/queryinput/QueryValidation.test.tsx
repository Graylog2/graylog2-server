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
import userEvent from '@testing-library/user-event';
import * as Immutable from 'immutable';

import { QueriesActions, QueryValidationState } from 'views/stores/QueriesStore';
import { asMock, StoreMock as MockStore } from 'helpers/mocking';

import QueryValidation from './QueryValidation';

jest.mock('views/stores/QueriesStore', () => ({
  QueriesActions: {
    validateQuery: jest.fn(() => Promise.resolve()),
  },
}));

jest.mock('views/stores/SearchStore', () => ({
  SearchStore: MockStore(
    'listen',
    ['getInitialState', () => ({ search: { parameters: [] } })],
  ),
}));

describe('QueryValidation', () => {
  const queryValidationError: QueryValidationState = {
    status: 'ERROR',
    explanations: [{
      index: 'example_index',
      message: {
        errorType: 'ParseException',
        errorMessage: "Cannot parse 'source: '",
      },
    }],
  };

  const queryValidationSuccess: QueryValidationState = {
    status: 'OK',
    explanations: undefined,
  };

  const validationErrorIconTitle = 'Toggle validation error explanation';

  afterEach(() => {
    jest.clearAllMocks();
  });

  it('should validate query on mount', async () => {
    render(<QueryValidation queryString="source:" timeRange={{ type: 'relative', from: 300 }} streams={['stream-id']} />);

    await waitFor(() => expect(QueriesActions.validateQuery).toHaveBeenCalledTimes(1));

    const expectedPayload = {
      queryString: 'source:',
      timeRange: { type: 'relative', from: 300 },
      streams: ['stream-id'],
      parameters: [],
      parameterBindings: Immutable.Map(),
    };

    expect(QueriesActions.validateQuery).toHaveBeenCalledWith(expectedPayload);
  });

  it('should validate query when changing query string', async () => {
    const { rerender } = render(<QueryValidation queryString="source:" timeRange={undefined} />);

    await waitFor(() => expect(QueriesActions.validateQuery).toHaveBeenCalledTimes(1));

    rerender(<QueryValidation queryString="updated query" timeRange={undefined} />);

    await waitFor(() => expect(QueriesActions.validateQuery).toHaveBeenCalledTimes(2));

    const expectedPayload = {
      queryString: 'updated query',
      timeRange: undefined,
      streams: undefined,
      parameters: [],
      parameterBindings: Immutable.Map(),
    };

    expect(QueriesActions.validateQuery).toHaveBeenCalledWith(expectedPayload);
  });

  it('should validate query when changing filter', async () => {
    const { rerender } = render(<QueryValidation queryString="source:host-1" timeRange={undefined} filter="http_method:" />);

    await waitFor(() => expect(QueriesActions.validateQuery).toHaveBeenCalledTimes(1));

    rerender(<QueryValidation queryString="source:host-1" timeRange={undefined} filter="http_method:GET" />);

    await waitFor(() => expect(QueriesActions.validateQuery).toHaveBeenCalledTimes(2));

    const expectedPayload = {
      queryString: 'source:host-1',
      timeRange: undefined,
      streams: undefined,
      filter: 'http_method:',
      parameters: [],
      parameterBindings: Immutable.Map(),
    };

    expect(QueriesActions.validateQuery).toHaveBeenCalledWith(expectedPayload);
  });

  it('should display validation error icon when there is a validation error', async () => {
    asMock(QueriesActions.validateQuery).mockReturnValue(Promise.resolve(queryValidationError));
    render(<QueryValidation queryString="source:" timeRange={undefined} />);

    await screen.findByTitle(validationErrorIconTitle);
  });

  it('should not display validation error icon when there is no validation error', async () => {
    asMock(QueriesActions.validateQuery).mockReturnValue(Promise.resolve(queryValidationError));

    const { rerender } = render(<QueryValidation queryString="source:" timeRange={undefined} />);
    await screen.findByTitle(validationErrorIconTitle);

    asMock(QueriesActions.validateQuery).mockReturnValue(Promise.resolve(queryValidationSuccess));
    rerender(<QueryValidation queryString="source:example.org" timeRange={undefined} />);

    await waitFor(() => expect(screen.queryByTitle(validationErrorIconTitle)).not.toBeInTheDocument());
  });

  it('should display validation error explanation', async () => {
    asMock(QueriesActions.validateQuery).mockReturnValue(Promise.resolve(queryValidationError));
    render(<QueryValidation queryString="source:" timeRange={undefined} />);

    const validationExplanationTrigger = await screen.findByTitle(validationErrorIconTitle);
    userEvent.click(validationExplanationTrigger);

    await screen.findByText('Error (ParseException)');
    await screen.findByText("Cannot parse 'source: '");
  });
});

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
import { Form, Formik } from 'formik';

import QueryValidation from 'views/components/searchbar/queryvalidation/QueryValidation';
import { asMock } from 'helpers/mocking';
import FormWarningsProvider from 'contexts/FormWarningsProvider';
import fetch from 'logic/rest/FetchProvider';
import SearchExecutionState from 'views/logic/search/SearchExecutionState'; import { QueryValidationState } from 'views/components/searchbar/queryvalidation/hooks/useValidateQuery';

jest.mock('views/stores/QueriesStore', () => ({
  QueriesActions: {
    validateQuery: jest.fn(() => Promise.resolve()),
  },
}));

jest.mock('views/stores/SearchStore', () => ({
  SearchStore: {
    listen: jest.fn(),
    getInitialState: jest.fn(() => ({ search: { parameters: [] } })),
  },
}));

const MockSearchExecutionState = new SearchExecutionState();

jest.mock('views/stores/SearchExecutionStateStore', () => ({
  SearchExecutionStateStore: {
    getInitialState: jest.fn(() => MockSearchExecutionState),
    listen: () => jest.fn(),
  },
}));

jest.mock('logic/rest/FetchProvider', () => jest.fn(() => Promise.resolve()));
jest.mock('logic/datetimes/DateTime', () => ({}));

describe('QueryValidation', () => {
  const errorResponse = {
    status: 'ERROR',
    explanations: [{
      error_type: 'ParseException',
      error_message: "Cannot parse 'source: '",
      begin_line: 1,
      end_line: 1,
      begin_column: 1,
      end_column: 5,
    }],
  };

  const queryValidationSuccess: QueryValidationState = {
    status: 'OK',
    explanations: undefined,
  };

  const validationErrorIconTitle = 'Toggle validation error explanation';

  const SUT = (props) => (
    <Formik onSubmit={() => {}} initialValues={{}}>
      <Form>
        <FormWarningsProvider>
          <QueryValidation {...props} />
        </FormWarningsProvider>
      </Form>
    </Formik>
  );

  afterEach(() => {
    jest.clearAllMocks();
  });

  it('should validate query on mount', async () => {
    render(<SUT queryString="source:"
                timeRange={{ type: 'relative', from: 300 }}
                streams={['stream-id']} />);

    await waitFor(() => expect(fetch).toHaveBeenCalledTimes(1));

    const expectedPayload = {
      query: 'source:',
      filter: undefined,
      timerange: { type: 'relative', from: 300 },
      streams: ['stream-id'],
      parameters: [],
      parameter_bindings: Immutable.Map(),
    };

    expect(fetch).toHaveBeenCalledWith('POST', expect.any(String), expectedPayload);
  });

  it('should validate query when changing query string', async () => {
    const { rerender } = render(<SUT queryString="source:" timeRange={undefined} />);

    await waitFor(() => expect(fetch).toHaveBeenCalledTimes(1));

    rerender(<SUT queryString="updated query" timeRange={undefined} />);

    await waitFor(() => expect(fetch).toHaveBeenCalledTimes(2));

    const expectedPayload = {
      query: 'updated query',
      filter: undefined,
      timerange: undefined,
      streams: undefined,
      parameters: [],
      parameter_bindings: Immutable.Map(),
    };

    expect(fetch).toHaveBeenCalledWith('POST', expect.any(String), expectedPayload);
  });

  it('should validate query when changing filter', async () => {
    const { rerender } = render(<SUT queryString="source:host-1" timeRange={undefined} filter="http_method:" />);

    await waitFor(() => expect(fetch).toHaveBeenCalledTimes(1));

    rerender(<SUT queryString="source:host-1" timeRange={undefined} filter="http_method:GET" />);

    await waitFor(() => expect(fetch).toHaveBeenCalledTimes(2));

    const expectedPayload = {
      query: 'source:host-1',
      timerange: undefined,
      streams: undefined,
      filter: 'http_method:',
      parameters: [],
      parameter_bindings: Immutable.Map(),
    };

    expect(fetch).toHaveBeenCalledWith('POST', expect.any(String), expectedPayload);
  });

  it('should display validation error icon when there is a validation error', async () => {
    asMock(fetch).mockReturnValue(Promise.resolve(errorResponse));
    render(<SUT queryString="source:" timeRange={undefined} />);

    await screen.findByTitle(validationErrorIconTitle);
  });

  it('should not display validation error icon when there is no validation error', async () => {
    asMock(fetch).mockReturnValue(Promise.resolve(errorResponse));

    const { rerender } = render(<SUT queryString="source:" timeRange={undefined} />);
    await screen.findByTitle(validationErrorIconTitle);

    asMock(fetch).mockReturnValue(Promise.resolve(queryValidationSuccess));
    rerender(<SUT queryString="source:example.org" timeRange={undefined} />);

    await waitFor(() => expect(screen.queryByTitle(validationErrorIconTitle)).not.toBeInTheDocument());
  });

  it('should display validation error explanation', async () => {
    asMock(fetch).mockReturnValue(Promise.resolve(errorResponse));
    render(<SUT queryString="source:" timeRange={undefined} />);

    const validationExplanationTrigger = await screen.findByTitle(validationErrorIconTitle);
    userEvent.click(validationExplanationTrigger);

    await screen.findByText('Error');
    await screen.findByText('ParseException');
    await screen.findByText(/Cannot parse 'source: '/);
  });
});

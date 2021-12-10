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
import { Form, Formik } from 'formik';

import QueryValidation from 'views/components/searchbar/queryvalidation/QueryValidation';
import SearchExecutionState from 'views/logic/search/SearchExecutionState';
import FormWarningsContext from 'contexts/FormWarningsContext'; import { QueryValidationState } from 'views/components/searchbar/queryvalidation/types';

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

type SUTProps = {
  // eslint-disable-next-line react/require-default-props
  error?: QueryValidationState,
  // eslint-disable-next-line react/require-default-props
  warning?: QueryValidationState,
}

describe('QueryValidation', () => {
  const errorResponse: QueryValidationState = {
    status: 'ERROR',
    explanations: [{
      errorType: 'ParseException',
      errorMessage: "Cannot parse 'source: '",
      beginLine: 1,
      endLine: 1,
      beginColumn: 1,
      endColumn: 5,
    }],
  };

  const validationErrorIconTitle = 'Toggle validation error explanation';

  const SUT = ({ error, warning }: SUTProps) => (
    <Formik onSubmit={() => {}} initialValues={{}} initialErrors={error ? { queryString: error } : {}}>
      <Form>
        <FormWarningsContext.Provider value={{ warnings: warning ? { queryString: warning } : {}, setFieldWarning: () => {} }}>
          <QueryValidation />
        </FormWarningsContext.Provider>
      </Form>
    </Formik>
  );

  afterEach(() => {
    jest.clearAllMocks();
  });

  it('should display validation error icon when there is a validation error', async () => {
    render(<SUT error={errorResponse} />);

    await screen.findByTitle(validationErrorIconTitle);
  });

  it('should not display validation error icon when there is no validation error', async () => {
    render(<SUT />);

    await waitFor(() => expect(screen.queryByTitle(validationErrorIconTitle)).not.toBeInTheDocument());
  });

  it('should display validation error explanation', async () => {
    render(<SUT error={errorResponse} />);

    const validationExplanationTrigger = await screen.findByTitle(validationErrorIconTitle);
    userEvent.click(validationExplanationTrigger);

    await screen.findByText('Error');
    await screen.findByText('ParseException');
    await screen.findByText(/Cannot parse 'source: '/);
  });
});

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
import { render, screen, waitFor } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';
import { Form, Formik } from 'formik';

import QueryValidation from 'views/components/searchbar/queryvalidation/QueryValidation';
import SearchExecutionState from 'views/logic/search/SearchExecutionState';
import FormWarningsContext from 'contexts/FormWarningsContext';
import type { QueryValidationState } from 'views/components/searchbar/queryvalidation/types';
import { validationError, validationErrorExplanation } from 'fixtures/queryValidationState';
import usePluginEntities from 'views/logic/usePluginEntities';

import asMock from '../../../../../test/helpers/mocking/AsMock';

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

jest.mock('views/logic/usePluginEntities');
jest.mock('logic/rest/FetchProvider', () => jest.fn(() => Promise.resolve()));
jest.mock('logic/datetimes/DateTime', () => ({}));

type SUTProps = {
  // eslint-disable-next-line react/require-default-props
  error?: QueryValidationState,
  // eslint-disable-next-line react/require-default-props
  warning?: QueryValidationState,
}

describe('QueryValidation', () => {
  const validationErrorIconTitle = 'Toggle validation error explanation';

  const openExplanation = async () => {
    const validationExplanationTrigger = await screen.findByTitle(validationErrorIconTitle);
    userEvent.click(validationExplanationTrigger);
  };

  const SUT = ({ error, warning }: SUTProps) => (
    <Formik onSubmit={() => {}} initialValues={{}} initialErrors={error ? { queryString: error } : {}} enableReinitialize>
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
    render(<SUT error={validationError} />);

    await screen.findByTitle(validationErrorIconTitle);
  });

  it('should not display validation error icon when there is no validation error', async () => {
    render(<SUT />);

    await waitFor(() => expect(screen.queryByTitle(validationErrorIconTitle)).not.toBeInTheDocument());
  });

  it('should display validation error explanation', async () => {
    render(<SUT error={validationError} />);

    await openExplanation();

    await screen.findByText('Error');
    await screen.findByText('Parse Exception');
    await screen.findByText(/Cannot parse 'source: '/);
  });

  it('should display validation error specific documentation links', async () => {
    render(<SUT error={validationError} />);

    await openExplanation();

    await screen.findByText('Parse Exception');
    await screen.findByTitle('Parse Exception documentation');
  });

  it('renders pluggable validation explanation', async () => {
    const ExampleComponent = ({ validationState }: { validationState: QueryValidationState }) => (
      <>Plugable validation explanation for {validationState.explanations.map(({ errorTitle }) => errorTitle).join()}</>
    );
    asMock(usePluginEntities).mockImplementation((entityKey) => (entityKey === 'views.elements.validationErrorExplanation' ? [ExampleComponent] : []));
    render(<SUT error={validationError} />);

    await openExplanation();

    await screen.findByText('Plugable validation explanation for Parse Exception');
  });

  it('only displays current validation explanation', async () => {
    const multipleValidationErrors: QueryValidationState = {
      status: 'ERROR',
      explanations: [validationErrorExplanation, { ...validationErrorExplanation, endColumn: 6 }],
    };
    const singleValidationError: QueryValidationState = {
      status: 'ERROR',
      explanations: [validationErrorExplanation],
    };

    const { rerender } = render(<SUT error={multipleValidationErrors} />);
    await openExplanation();

    await waitFor(() => expect(screen.getAllByText('Parse Exception')).toHaveLength(2));

    rerender(<SUT error={singleValidationError} />);

    await waitFor(() => expect(screen.getAllByText('Parse Exception')).toHaveLength(1));
  });
});

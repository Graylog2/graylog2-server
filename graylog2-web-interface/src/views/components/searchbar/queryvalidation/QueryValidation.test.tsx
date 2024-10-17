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
import FormWarningsContext from 'contexts/FormWarningsContext';
import type { QueryValidationState } from 'views/components/searchbar/queryvalidation/types';
import { validationError, validationErrorExplanation } from 'fixtures/queryValidationState';
import usePluginEntities from 'hooks/usePluginEntities';
import asMock from 'helpers/mocking/AsMock';

jest.mock('hooks/usePluginEntities');
jest.mock('logic/rest/FetchProvider', () => jest.fn(() => Promise.resolve()));

type SUTProps = {

  error?: QueryValidationState,

  warning?: QueryValidationState,
}

describe('QueryValidation', () => {
  const validationErrorIconTitle = /Toggle validation/;

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
    await screen.findByTitle('Query error documentation');
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
      explanations: [validationErrorExplanation, { ...validationErrorExplanation, id: 'validation-explanation-id-2' }],
      context: {
        searched_index_ranges: [],
      },
    };
    const singleValidationError: QueryValidationState = {
      status: 'ERROR',
      explanations: [validationErrorExplanation],
      context: {
        searched_index_ranges: [],
      },
    };

    const { rerender } = render(<SUT error={multipleValidationErrors} />);
    await openExplanation();

    await waitFor(() => expect(screen.getAllByText('Parse Exception')).toHaveLength(2));

    rerender(<SUT error={singleValidationError} />);

    await waitFor(() => expect(screen.getAllByText('Parse Exception')).toHaveLength(1));
  });

  it('should deduplicate "unknown field" errors referring to same field name', async () => {
    const validationErrorForUnknownField: QueryValidationState = {
      status: 'WARNING',
      explanations: [{
        id: 'foo',
        errorType: 'UNKNOWN_FIELD',
        beginLine: 1,
        beginColumn: 2,
        endLine: 1,
        endColumn: 16,
        errorTitle: 'Unknown field',
        errorMessage: 'Query contains unknown field: TargetFilename',
        relatedProperty: 'TargetFilename',
      }, {
        id: 'bar',
        errorType: 'UNKNOWN_FIELD',
        beginLine: 1,
        beginColumn: 193,
        endLine: 1,
        endColumn: 207,
        errorTitle: 'Unknown field',
        errorMessage: 'Query contains unknown field: TargetFilename',
        relatedProperty: 'TargetFilename',
      }],
      context: {
        searched_index_ranges: [],
      },
    };
    render(<SUT error={validationErrorForUnknownField} />);

    await openExplanation();

    const explanations = await screen.findAllByText(/Query contains unknown field: TargetFilename/i);

    expect(explanations.length).toBe(1);
  });
});

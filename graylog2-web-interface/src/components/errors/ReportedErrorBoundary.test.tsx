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
import { render, waitFor, fireEvent, screen } from 'wrappedTestingLibrary';

import suppressConsole from 'helpers/suppressConsole';
import ErrorsActions from 'actions/errors/ErrorsActions';
import { createReactError, createUnauthorizedError, createNotFoundError } from 'logic/errors/ReportedErrors';
import FetchError from 'logic/errors/FetchError';
import { Link } from 'components/common/router';
import useProductName, { DEFAULT_PRODUCT_NAME } from 'brand-customization/useProductName';
import asMock from 'helpers/mocking/AsMock';

import ReportedErrorBoundary from './ReportedErrorBoundary';

jest.mock('routing/withLocation', () => (Component) => (props) => (
  <Component {...props} location={{ pathname: '/' }} />
));
jest.mock('brand-customization/useProductName');

const triggerError: typeof ErrorsActions.report = async (error) => {
  await suppressConsole(() => {
    ErrorsActions.report(error);
  });
  await waitFor(() => expect(screen.queryByText('Hello World!')).toBeNull());
};

describe('ReportedErrorBoundary', () => {
  beforeEach(() => {
    asMock(useProductName).mockReturnValue(DEFAULT_PRODUCT_NAME);
  });

  describe('displays', () => {
    beforeEach(async () => {
      // eslint-disable-next-line testing-library/no-render-in-lifecycle
      render(<ReportedErrorBoundary>Hello World!</ReportedErrorBoundary>);

      await screen.findByText('Hello World!');
    });

    it('displays runtime error page when react error got reported', async () => {
      await triggerError(createReactError(new Error('The error message'), { componentStack: 'The component stack' }));

      await screen.findByText('Something went wrong.');
      await screen.findByText('The error message');
    });

    it('displays not found page when not found error got reported', async () => {
      const response = { status: 404, body: { message: 'The request error message' } };

      await triggerError(createNotFoundError(new FetchError('The request error message', response.status, response)));

      await screen.findByText('Page not found');
      await screen.findByText(/The page you are looking for does not exist/i);
    });

    it('displays reported error with an unknown type', async () => {
      const response = { status: 404, body: { message: 'The error message' } };

      await triggerError({
        ...createNotFoundError(new FetchError('The error message', response.status, response)),
        type: 'UnknownReportedError',
      });

      await screen.findByText('Something went wrong');
      await screen.findByText(/The error message/);
    });

    it('displays unauthorized error page when unauthorized error got reported', async () => {
      const response = { status: 403, body: { message: 'The request error message' } };

      await triggerError(
        createUnauthorizedError(new FetchError('The request error message', response.status, response)),
      );

      await screen.findByText('Missing Permissions');

      expect(screen.getByText(/The request error message/)).toBeInTheDocument();
    });

    it('displays helpful product sources', async () => {
      await triggerError(createReactError(new Error('The error message'), { componentStack: 'The component stack' }));

      await screen.findByText(/Do not hesitate to consult the Graylog community if your questions are not answered/i);
    });

    it('does not show support sources if product name is not default', async () => {
      asMock(useProductName).mockReturnValue('AwesomeLog');

      await triggerError(createReactError(new Error('The error message'), { componentStack: 'The component stack' }));

      await screen.findByText(/Something went wrong/i);

      expect(
        screen.queryByText(/Do not hesitate to consult the Graylog community if your questions are not answered/i),
      ).not.toBeInTheDocument();
    });
  });

  it('resets error when navigation changes', async () => {
    render(
      <>
        <Link to="/">Go back</Link>
        <ReportedErrorBoundary>Hello World!</ReportedErrorBoundary>
      </>,
    );

    await screen.findByText('Hello World!');

    const response = { status: 403, body: { message: 'The request error message' } };

    expect(screen.getByText('Hello World!')).not.toBeNull();

    await triggerError(createUnauthorizedError(new FetchError('The request error message', response.status, response)));

    await screen.findByText('Missing Permissions');

    const link = await screen.findByRole('link', { name: 'Go back' });
    fireEvent.click(link);

    await screen.findByText('Hello World!');
  });
});

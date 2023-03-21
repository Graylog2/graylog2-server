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

import ReportedErrorBoundary from './ReportedErrorBoundary';

jest.mock('routing/withLocation', () => (Component) => (props) => <Component {...props} location={{ pathname: '/' }} />);

describe('ReportedErrorBoundary', () => {
  it('displays child component if there is no error', async () => {
    render(<ReportedErrorBoundary>Hello World!</ReportedErrorBoundary>);

    await screen.findByText('Hello World!');
  });

  it('displays runtime error page when react error got reported', async () => {
    render(<ReportedErrorBoundary>Hello World!</ReportedErrorBoundary>);

    await screen.findByText('Hello World!');

    await suppressConsole(() => {
      ErrorsActions.report(createReactError(new Error('The error message'), { componentStack: 'The component stack' }));
    });

    await waitFor(() => expect(screen.queryByText('Hello World!')).toBeNull());

    expect(screen.getByText('Something went wrong.')).not.toBeNull();
    expect(screen.getByText('The error message')).not.toBeNull();
  });

  it('displays not found page when not found error got reported', async () => {
    render(<ReportedErrorBoundary>Hello World!</ReportedErrorBoundary>);

    await screen.findByText('Hello World!');

    const response = { status: 404, body: { message: 'The request error message' } };

    await suppressConsole(() => {
      ErrorsActions.report(createNotFoundError(new FetchError('The request error message', response.status, response)));
    });

    await waitFor(() => expect(screen.queryByText('Hello World!')).toBeNull());

    expect(screen.getByText('Page not found')).not.toBeNull();
    expect(screen.getByText('The party gorilla was just here, but had another party to rock.')).not.toBeNull();
  });

  it('displays reported error with an unkown type', async () => {
    render(<ReportedErrorBoundary>Hello World!</ReportedErrorBoundary>);

    await screen.findByText('Hello World!');

    const response = { status: 404, body: { message: 'The error message' } };

    await suppressConsole(() => {
      ErrorsActions.report({ ...createNotFoundError(new FetchError('The error message', response.status, response)), type: 'UnkownReportedError' });
    });

    await waitFor(() => expect(screen.queryByText('Hello World!')).toBeNull());

    expect(screen.getByText('Something went wrong')).not.toBeNull();
    expect(screen.getByText(/The error message/)).not.toBeNull();
  });

  it('displays unauthorized error page when unauthorized error got reported', async () => {
    render(<ReportedErrorBoundary>Hello World!</ReportedErrorBoundary>);

    await screen.findByText('Hello World!');

    const response = { status: 403, body: { message: 'The request error message' } };

    await suppressConsole(() => {
      ErrorsActions.report(createUnauthorizedError(new FetchError('The request error message', response.status, response)));
    });

    await screen.findByText('Missing Permissions');

    expect(screen.queryByText('Hello World!')).toBeNull();
    expect(screen.getByText(/The request error message/)).toBeInTheDocument();
  });

  it('resets error when navigation changes', async () => {
    render((
      <>
        <Link to="/">Go back</Link>
        <ReportedErrorBoundary>Hello World!</ReportedErrorBoundary>
      </>
    ));

    await screen.findByText('Hello World!');

    const response = { status: 403, body: { message: 'The request error message' } };

    expect(screen.getByText('Hello World!')).not.toBeNull();

    await suppressConsole(() => {
      ErrorsActions.report(createUnauthorizedError(new FetchError('The request error message', response.status, response)));
    });

    await screen.findByText('Missing Permissions');

    const link = await screen.findByRole('link', { name: 'Go back' });
    fireEvent.click(link);

    await screen.findByText('Hello World!');
  });
});

// @flow strict
import React from 'react';
import { render, waitFor } from 'wrappedTestingLibrary';
import { act } from 'react-dom/test-utils';
import suppressConsole from 'helpers/suppressConsole';

import history from 'util/History';
import ErrorsActions from 'actions/errors/ErrorsActions';
import { createReactError, createUnauthorizedError, createNotFoundError } from 'logic/errors/ReportedErrors';
import { FetchError } from 'logic/rest/FetchProvider';

import ReportedErrorBoundary from './ReportedErrorBoundary';

jest.unmock('logic/rest/FetchProvider');
jest.mock('routing/withLocation', () => (Component) => (props) => <Component {...props} location={{ pathname: '/' }} />);

const router = {
  listen: () => jest.fn(),
};

describe('ReportedErrorBoundary', () => {
  it('displays child component if there is no error', () => {
    const { getByText } = render(<ReportedErrorBoundary router={router}>Hello World!</ReportedErrorBoundary>);

    expect(getByText('Hello World!')).not.toBeNull();
  });

  it('displays runtime error page when react error got reported', async () => {
    const { getByText, queryByText } = render(<ReportedErrorBoundary router={router}>Hello World!</ReportedErrorBoundary>);

    suppressConsole(() => {
      ErrorsActions.report(createReactError(new Error('The error message'), { componentStack: 'The component stack' }));
    });

    await waitFor(() => expect(queryByText('Hello World!')).toBeNull());

    expect(getByText('Something went wrong.')).not.toBeNull();
    expect(getByText('The error message')).not.toBeNull();
  });

  it('displays not found page when not found error got reported', async () => {
    const { getByText, queryByText } = render(<ReportedErrorBoundary router={router}>Hello World!</ReportedErrorBoundary>);
    const response = { status: 404, body: { message: 'The request error message' } };

    suppressConsole(() => {
      ErrorsActions.report(createNotFoundError(new FetchError('The request error message', response)));
    });

    await waitFor(() => expect(queryByText('Hello World!')).toBeNull());

    expect(getByText('Page not found')).not.toBeNull();
    expect(getByText('The party gorilla was just here, but had another party to rock.')).not.toBeNull();
  });

  it('displays reported error with an unkown type', async () => {
    const { getByText, queryByText } = render(<ReportedErrorBoundary router={router}>Hello World!</ReportedErrorBoundary>);
    const response = { status: 404, body: { message: 'The error message' } };

    suppressConsole(() => {
      ErrorsActions.report({ ...createNotFoundError(new FetchError('The error message', response)), type: 'UnkownReportedError' });
    });

    await waitFor(() => expect(queryByText('Hello World!')).toBeNull());

    expect(getByText('Something went wrong')).not.toBeNull();
    expect(getByText(/The error message/)).not.toBeNull();
  });

  it('displays unauthorized error page when unauthorized error got reported', async () => {
    const { findByText, queryByText } = render(<ReportedErrorBoundary router={router}>Hello World!</ReportedErrorBoundary>);
    const response = { status: 403, body: { message: 'The request error message' } };

    suppressConsole(() => {
      ErrorsActions.report(createUnauthorizedError(new FetchError('The request error message', response)));
    });

    await findByText('Missing Permissions');

    expect(queryByText('Hello World!')).toBeNull();
    expect(queryByText(/The request error message/)).toBeInTheDocument();
  });

  it('resets error when navigation changes', async () => {
    const { getByText } = render(<ReportedErrorBoundary>Hello World!</ReportedErrorBoundary>);
    const response = { status: 403, body: { message: 'The request error message' } };

    expect(getByText('Hello World!')).not.toBeNull();

    suppressConsole(() => {
      ErrorsActions.report(createUnauthorizedError(new FetchError('The request error message', response)));
    });

    await waitFor(() => expect(getByText('Missing Permissions')).not.toBeNull());

    act(() => history.push('/'));

    await waitFor(() => expect(getByText('Hello World!')).not.toBeNull());
  });
});

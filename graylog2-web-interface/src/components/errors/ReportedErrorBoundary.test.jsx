// @flow strict
import React from 'react';
import { render, cleanup, wait } from 'wrappedTestingLibrary';
import { act } from 'react-dom/test-utils';

import suppressConsole from 'helpers/suppressConsole';
import ErrorsActions from 'actions/errors/ErrorsActions';
import { createReactError, createUnauthorizedError } from 'logic/errors/ReportedErrors';
import { FetchError } from 'logic/rest/FetchProvider';
import ReportedErrorBoundary from './ReportedErrorBoundary';

jest.unmock('logic/rest/FetchProvider');
jest.mock('react-router', () => ({ withRouter: (x) => x }));

const router = {
  listen: () => jest.fn(),
};

describe('ReportedErrorBoundary', () => {
  afterEach(() => {
    cleanup();
  });

  it('registers to router upon mount', () => {
    const mockRouter = {
      listen: jest.fn(() => jest.fn()),
    };
    render(<ReportedErrorBoundary router={mockRouter}>Hello World!</ReportedErrorBoundary>);

    expect(mockRouter.listen).toHaveBeenCalledTimes(1);
  });

  it('unregisters from router upon unmount', () => {
    const unlisten = jest.fn();
    const mockRouter = {
      listen: () => unlisten,
    };
    const { unmount } = render(<ReportedErrorBoundary router={mockRouter}>Hello World!</ReportedErrorBoundary>);

    unmount();

    expect(unlisten).toHaveBeenCalled();
  });


  it('displays child component if there is no error', () => {
    const { getByText } = render(<ReportedErrorBoundary router={router}>Hello World!</ReportedErrorBoundary>);

    expect(getByText('Hello World!')).not.toBeNull();
  });

  it('displays runtime error page when react error got reported', async () => {
    const { getByText, queryByText } = render(<ReportedErrorBoundary router={router}>Hello World!</ReportedErrorBoundary>);

    suppressConsole(() => {
      ErrorsActions.report(createReactError(new Error('The error message'), { componentStack: 'The component stack' }));
    });
    await wait(() => expect(queryByText('Hello World!')).toBeNull());
    await wait(() => expect(getByText('Something went wrong.')).not.toBeNull());
    await wait(() => expect(getByText('The error message')).not.toBeNull());
  });

  it('displays unauthorized error page when unauthorized error got reported', async () => {
    const { getByText, queryByText } = render(<ReportedErrorBoundary router={router}>Hello World!</ReportedErrorBoundary>);

    suppressConsole(() => {
      ErrorsActions.report(createUnauthorizedError(new FetchError('The request error message', new Error('The request error message'))));
    });

    await wait(() => expect(queryByText('Hello World!')).toBeNull());
    await wait(() => expect(getByText('Missing Permissions')).not.toBeNull());
    await wait(() => expect(getByText(/The request error message/)).not.toBeNull());
  });

  it('resets error when navigation changes', async () => {
    const mockRouter = {
      listen: jest.fn(() => jest.fn()),
    };

    const { getByText } = render(<ReportedErrorBoundary router={mockRouter}>Hello World!</ReportedErrorBoundary>);

    expect(getByText('Hello World!')).not.toBeNull();

    suppressConsole(() => {
      ErrorsActions.report(createUnauthorizedError(new FetchError('The request error message', new Error('The request error message'))));
    });

    await wait(() => expect(getByText('Missing Permissions')).not.toBeNull());
    const listenCallback = mockRouter.listen.mock.calls[1][0];
    act(() => listenCallback());

    await wait(() => expect(getByText('Hello World!')).not.toBeNull());
  });
});

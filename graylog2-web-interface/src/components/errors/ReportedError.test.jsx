// @flow strict
import React from 'react';
import { render, cleanup, waitForElement, waitForElementToBeRemoved } from 'wrappedTestingLibrary';
import { act } from 'react-dom/test-utils';

import suppressConsole from 'helpers/suppressConsole';
import ErrorsActions from 'actions/errors/ErrorsActions';
import { createReactError, createUnauthorizedError } from 'logic/errors/ReportedErrors';
import { FetchError } from 'logic/rest/FetchProvider';
import ReportedError from './ReportedError';

jest.unmock('logic/rest/FetchProvider');
jest.mock('react-router', () => ({ withRouter: (x) => x }));

const router = {
  listen: () => jest.fn(),
};

describe('ReportedError', () => {
  afterEach(() => {
    cleanup();
  });

  it('registers to router upon mount', () => {
    const mockRouter = {
      listen: jest.fn(() => jest.fn()),
    };
    render(<ReportedError router={mockRouter}>Hello World!</ReportedError>);

    expect(mockRouter.listen).toHaveBeenCalledTimes(1);
  });

  it('unregisters from router upon unmount', () => {
    const unlisten = jest.fn();
    const mockRouter = {
      listen: () => unlisten,
    };
    const { unmount } = render(<ReportedError router={mockRouter}>Hello World!</ReportedError>);

    unmount();

    expect(unlisten).toHaveBeenCalled();
  });


  it('displays child component if there is no error', () => {
    const { getByText } = render(<ReportedError router={router}>Hello World!</ReportedError>);

    expect(getByText('Hello World!')).not.toBeNull();
  });

  it('displays runtime error page when react error got reported', async () => {
    const { getByText } = render(<ReportedError router={router}>Hello World!</ReportedError>);

    suppressConsole(() => {
      ErrorsActions.report(createReactError(new Error('The error message'), { componentStack: 'The component stack' }));
    });

    await waitForElementToBeRemoved(() => getByText('Hello World!'));
    await waitForElement(() => getByText('Something went wrong.'));
    await waitForElement(() => getByText('The error message'));
  });

  it('displays unauthorized error page when unauthorized error got reported', async () => {
    const { getByText } = render(<ReportedError router={router}>Hello World!</ReportedError>);

    suppressConsole(() => {
      ErrorsActions.report(createUnauthorizedError(new FetchError('The request error message', new Error('The request error message'))));
    });

    await waitForElementToBeRemoved(() => getByText('Hello World!'));
    await waitForElement(() => getByText('Missing Permissions'));
    await waitForElement(() => getByText(/The request error message/));
  });

  it('resets error when navigation changes', async () => {
    const mockRouter = {
      listen: jest.fn(() => jest.fn()),
    };

    const { getByText } = render(<ReportedError router={mockRouter}>Hello World!</ReportedError>);

    expect(getByText('Hello World!')).not.toBeNull();

    suppressConsole(async () => {
      ErrorsActions.report(createUnauthorizedError(new FetchError('The request error message', new Error('The request error message'))));
    });

    await waitForElement(() => getByText('Missing Permissions'));
    const listenCallback = mockRouter.listen.mock.calls[1][0];
    act(() => listenCallback());

    await waitForElement(() => getByText('Hello World!'));
  });
});

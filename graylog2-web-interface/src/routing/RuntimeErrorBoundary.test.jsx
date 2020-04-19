// @flow strict
import React from 'react';
import { render } from 'wrappedTestingLibrary';

import suppressConsole from 'helpers/suppressConsole';
import ErrorsActions from 'actions/errors/ErrorsActions';
import { ReactErrorType } from 'logic/errors/ReportedError';
import RuntimeErrorBoundary from './RuntimeErrorBoundary';

jest.mock('actions/errors/ErrorsActions', () => ({
  report: jest.fn(),
}));

const ErroneusComponent = () => {
  // eslint-disable-next-line no-throw-literal
  throw {
    message: 'Oh no, a banana peel fell on the party gorilla\'s head!',
    stack: 'This the stack trace.',
  };
};

const WorkingComponent = () => <div>Hello World!</div>;

describe('RuntimeErrorBoundary', () => {
  it('displays child component', () => {
    const { getByText } = render(
      <RuntimeErrorBoundary>
        <WorkingComponent />
      </RuntimeErrorBoundary>,
    );

    expect(getByText('Hello World!')).not.toBe(null);
  });

  it('calls display error action after catching', () => {
    suppressConsole(() => {
      render(
        <RuntimeErrorBoundary>
          <ErroneusComponent />
        </RuntimeErrorBoundary>,
      );

      expect(ErrorsActions.report).toHaveBeenCalledTimes(1);
      expect(ErrorsActions.report.mock.calls[0][0].error).toStrictEqual({
        message: 'Oh no, a banana peel fell on the party gorilla\'s head!',
        stack: 'This the stack trace.',
      });
      expect(ErrorsActions.report.mock.calls[0][0].type).toEqual(ReactErrorType);
      expect(ErrorsActions.report.mock.calls[0][0].componentStack).not.toBeNull();
    });
  });
});

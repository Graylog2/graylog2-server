// @flow strict
import React from 'react';
import { render } from 'wrappedTestingLibrary';

import suppressConsole from 'helpers/suppressConsole';
import ErrorsActions from 'actions/errors/ErrorsActions';
import AppError from 'logic/errors/AppError';
import RuntimeErrorBoundary from './RuntimeErrorBoundary';

jest.mock('actions/errors/ErrorsActions', () => ({
  displayError: jest.fn(),
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

      expect(ErrorsActions.displayError).toHaveBeenCalledTimes(1);
      expect(ErrorsActions.displayError.mock.calls[0][0].error).toStrictEqual({
        message: 'Oh no, a banana peel fell on the party gorilla\'s head!',
        stack: 'This the stack trace.',
      });
      expect(ErrorsActions.displayError.mock.calls[0][0].type).toEqual(AppError.Type.Runtime);
      expect(ErrorsActions.displayError.mock.calls[0][0].componentStack).not.toBeNull();
    });
  });
});

// @flow strict
import React from 'react';
import { render } from 'wrappedTestingLibrary';
import suppressConsole from 'helpers/suppressConsole';

import { GlobalStylesContext } from 'contexts/GlobalStylesProvider';

import RouterErrorBoundary from './RouterErrorBoundary';

jest.mock('react-router', () => ({ withRouter: (x) => x }));

const ErroneusComponent = () => {
  // eslint-disable-next-line no-throw-literal
  throw {
    message: 'Oh no, a banana peel fell on the party gorilla\'s head!',
    stack: 'This the stack trace.',
  };
};

const WorkingComponent = () => <div>Hello World!</div>;

describe('RouterErrorBoundary', () => {
  const renderSUT = (children) => {
    const addGlobalStyles = jest.fn();

    return render(
      <GlobalStylesContext.Provider value={{ addGlobalStyles }}>
        {children}
      </GlobalStylesContext.Provider>,
    );
  };

  it('displays child component if there is no error', () => {
    const { getByText } = render(
      <RouterErrorBoundary>
        <WorkingComponent />
      </RouterErrorBoundary>,
    );

    expect(getByText('Hello World!')).not.toBeNull();
  });

  it('displays error after catching', () => {
    suppressConsole(() => {
      const { getByText } = renderSUT(
        <RouterErrorBoundary>
          <ErroneusComponent />
        </RouterErrorBoundary>,
      );

      expect(getByText('Oh no, a banana peel fell on the party gorilla\'s head!')).not.toBeNull();
    });
  });
});

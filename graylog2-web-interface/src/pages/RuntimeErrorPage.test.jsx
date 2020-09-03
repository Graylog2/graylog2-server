// @flow strict
import React from 'react';
import { render, fireEvent, waitFor } from 'wrappedTestingLibrary';

import { GlobalStylesContext } from 'contexts/GlobalStylesProvider';

import RuntimeErrorPage from './RuntimeErrorPage';

describe('RuntimeErrorPage', () => {
  const renderSUT = (children) => {
    const addGlobalStyles = jest.fn();

    return render(
      <GlobalStylesContext.Provider value={{ addGlobalStyles }}>
        {children}
      </GlobalStylesContext.Provider>,
    );
  };

  const SimpleRuntimeErrorPage = () => <RuntimeErrorPage error={new Error('The error message')} componentStack="The component stack" />;

  it('displays runtime error', () => {
    const { getByText } = renderSUT(<SimpleRuntimeErrorPage />);

    expect(getByText('Something went wrong.')).not.toBeNull();
    expect(getByText('The error message')).not.toBeNull();
  });

  it('displays component stack', async () => {
    const { getByText, queryByText } = renderSUT(<SimpleRuntimeErrorPage />);

    expect(getByText('Something went wrong.')).not.toBeNull();
    expect(queryByText('The component stack')).toBeNull();

    const showMoreButton = getByText('Show more');

    fireEvent.click(showMoreButton);

    waitFor(() => expect(getByText('The component stack')).not.toBeNull());
  });
});

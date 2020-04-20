// @flow strict
import React from 'react';
import { render, cleanup, fireEvent, waitForElement } from 'wrappedTestingLibrary';
import suppressConsole from 'helpers/suppressConsole';

import RuntimeErrorPage from './RuntimeErrorPage';

describe('RuntimeErrorPage', () => {
  afterEach(cleanup);

  const SimpleRuntimeErrorPage = () => <RuntimeErrorPage error={new Error('The error message')} componentStack="The component stack" />;

  it('displays runtime error', () => {
    suppressConsole(() => {
      const { getByText } = render(<SimpleRuntimeErrorPage />);

      expect(getByText('Something went wrong.')).not.toBeNull();
      expect(getByText('The error message')).not.toBeNull();
    });
  });

  it('displays component stack', () => {
    suppressConsole(async () => {
      const { getByText, queryByText } = render(<SimpleRuntimeErrorPage />);

      expect(getByText('Something went wrong.')).not.toBeNull();
      expect(queryByText('The component stack')).toBeNull();

      const showMoreButton = getByText('Show more');
      fireEvent.click(showMoreButton);

      await waitForElement(() => getByText('The component stack'));
    });
  });
});

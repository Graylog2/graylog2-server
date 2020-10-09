// @flow strict
import * as React from 'react';
import { render } from 'wrappedTestingLibrary';

import GettingStarted from './GettingStarted';

describe('GettingStarted', () => {
  it('should display description and select', () => {
    const { queryByText, queryByLabelText } = render(<GettingStarted />);

    expect(queryByText(/Select an authentication services to setup a new one./)).not.toBeNull();
    expect(queryByLabelText('Select a service')).not.toBeNull();
  });
});

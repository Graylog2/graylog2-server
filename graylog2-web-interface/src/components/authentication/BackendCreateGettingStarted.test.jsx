// @flow strict
import * as React from 'react';
import { render } from 'wrappedTestingLibrary';

import BackendCreateGettingStarted from './BackendCreateGettingStarted';

describe('BackendCreateGettingStarted', () => {
  it('should display description and select', () => {
    const { queryByText, queryByLabelText } = render(<BackendCreateGettingStarted />);

    expect(queryByText(/Select an authentication services to setup a new one./)).not.toBeNull();
    expect(queryByLabelText('Select a provider')).not.toBeNull();
  });
});

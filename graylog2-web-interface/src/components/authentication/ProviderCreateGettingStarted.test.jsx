// @flow strict
import * as React from 'react';
import { render } from 'wrappedTestingLibrary';

import ProviderCreateGettingStarted from './ProviderCreateGettingStarted';

describe('ProviderCreateGettingStarted', () => {
  it('should display description and select', () => {
    const { queryByText, queryByLabelText } = render(<ProviderCreateGettingStarted />);

    expect(queryByText(/Select an authentication provider to setup a new one./)).not.toBeNull();
    expect(queryByLabelText('Select a provider')).not.toBeNull();
  });
});

// @flow strict
import * as React from 'react';
import { render } from 'wrappedTestingLibrary';

import AppContentGrid from './AppContentGrid';

describe('AppContentGrid', () => {
  it('renders its children', async () => {
    const { getByText } = render(<AppContentGrid><div>The content</div></AppContentGrid>);
    expect(getByText('The content')).not.toBeNull();
  });
});

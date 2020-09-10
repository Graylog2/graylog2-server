// @flow strict
import * as React from 'react';
import { render } from 'wrappedTestingLibrary';

import EmptyEntity from './EmptyEntity';

describe('<EmptyEntity />', () => {
  it('should render children and default title', () => {
    const { getByText } = render(<EmptyEntity>The children</EmptyEntity>);

    expect(getByText('Looks like there is nothing here, yet!')).not.toBeNull();
    expect(getByText('The children')).not.toBeNull();
  });

  it('should render custom title', () => {
    const { getByText } = render(<EmptyEntity title="The custom title">The children</EmptyEntity>);

    expect(getByText('The custom title')).not.toBeNull();
  });
});

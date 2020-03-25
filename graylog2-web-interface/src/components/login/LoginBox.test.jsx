import React from 'react';
import { render } from 'wrappedTestingLibrary';

import LoginBox from './LoginBox';

describe('LoginBox', () => {
  it('renders a button after the input if buttonAfter is passed', () => {
    const { container } = render(<LoginBox><div /></LoginBox>);
    expect(container).toMatchSnapshot();
  });
});

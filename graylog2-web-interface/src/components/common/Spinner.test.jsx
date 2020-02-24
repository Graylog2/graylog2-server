import React from 'react';
import { render, cleanup } from 'wrappedTestingLibrary';

import Spinner from 'components/common/Spinner';

describe('<Spinner />', () => {
  afterEach(() => {
    cleanup();
  });

  it('should render without props', () => {
    const { getByText } = render(<Spinner />);
    expect(getByText('Loading...')).not.toBeNull();
  });

  it('should render with a different text string', () => {
    const text = 'Hello world!';
    const { getByText } = render(<Spinner text={text} />);
    expect(getByText(text)).not.toBeNull();
  });
});

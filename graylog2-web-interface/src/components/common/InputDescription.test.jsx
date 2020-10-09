import React from 'react';
import { render } from 'wrappedTestingLibrary';

import InputDescription from './InputDescription';

describe('<InputDescription />', () => {
  it('should render error message', () => {
    const { getByText } = render(<InputDescription error="The error message" />);

    expect(getByText('The error message')).toBeInTheDocument();
  });

  it('should render help text', () => {
    const { getByText } = render(<InputDescription help="The help text" />);

    expect(getByText('The help text')).toBeInTheDocument();
  });

  it('should render help and error message', () => {
    const { getByText } = render(<InputDescription help="The help text" error="The error message" />);

    expect(getByText(/The help text/)).toBeInTheDocument();
    expect(getByText(/The error message/)).toBeInTheDocument();
  });
});

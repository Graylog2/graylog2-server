import React from 'react';
import { render, cleanup } from 'wrappedTestingLibrary';

import ProgressBar from './ProgressBar';

describe('<ProgressBar />', () => {
  afterEach(() => {
    cleanup();
  });
  it('properly renders with no props', () => {
    const { container } = render(<ProgressBar />);

    expect(container).toMatchSnapshot();
  });

  it('properly renders with one bar', () => {
    const { container } = render(<ProgressBar bars={[{ value: 35 }]} />);

    expect(container).toMatchSnapshot();
  });

  it('properly renders with multiple bars', () => {
    const { container } = render(<ProgressBar bars={[{ value: 35 }, { value: 55 }]} />);

    expect(container).toMatchSnapshot();
  });

  it('properly renders with label', () => {
    const { container } = render(<ProgressBar bars={[{ value: 23, label: 'Example ProgressBar' }]} />);

    expect(container).toMatchSnapshot();
  });

  it('properly renders with animated', () => {
    const { container } = render(<ProgressBar bars={[{ value: 45, animated: true }]} />);

    expect(container).toMatchSnapshot();
  });

  it('properly renders with striped', () => {
    const { container } = render(<ProgressBar bars={[{ value: 56, striped: true }]} />);

    expect(container).toMatchSnapshot();
  });

  it('properly renders with bsStyle variant', () => {
    const { container } = render(<ProgressBar bars={[{ value: 67, bsStyle: 'danger' }]} />);

    expect(container).toMatchSnapshot();
  });
});

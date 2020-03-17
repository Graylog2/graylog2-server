import React from 'react';
import { render, cleanup } from 'wrappedTestingLibrary';
import { act } from 'react-dom/test-utils';

import Spinner from 'components/common/Spinner';

jest.useFakeTimers();

describe('<Spinner />', () => {
  afterEach(() => {
    cleanup();
  });

  it('should render without props', () => {
    const { getByText } = render(<Spinner delay={0} />);
    expect(getByText('Loading...')).not.toBeNull();
  });

  it('should render with a different text string', () => {
    const text = 'Hello world!';
    const { getByText } = render(<Spinner text={text} delay={0} />);
    expect(getByText(text)).not.toBeNull();
  });

  it('should not be visible initially', () => {
    const { queryByText } = render(<Spinner />);
    expect(queryByText('Loading ...')).toBeNull();
  });

  it('should be visible after when delay is completed', () => {
    const { container } = render(<Spinner />);
    act(() => {
      jest.advanceTimersByTime(200);
    });

    expect(container.firstChild).toHaveStyle('visibility: visible');
  });
});

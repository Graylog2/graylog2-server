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
    const { getByText } = render(<Spinner />);
    expect(getByText('Loading...')).not.toBeNull();
  });

  it('should render with a different text string', () => {
    const text = 'Hello world!';
    const { getByText } = render(<Spinner text={text} />);
    expect(getByText(text)).not.toBeNull();
  });

  it('should not be visible initially', () => {
    const { container } = render(<Spinner />);
    expect(container.firstChild).toHaveStyle('visibility: hidden');
  });

  it('should be visible after when delay is completed', () => {
    const { container } = render(<Spinner />);
    act(() => {
      jest.advanceTimersByTime(200);
    });

    expect(container.firstChild).toHaveStyle('visibility: visible');
  });

  it('should forward additional props to its icon', () => {
    const { getByTestId } = render(<Spinner data-testid="icon-test-id" />);
    expect(getByTestId('icon-test-id')).not.toBeNull();
  });
});

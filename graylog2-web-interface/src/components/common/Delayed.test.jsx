// @flow strict
import * as React from 'react';
import { cleanup, render } from 'wrappedTestingLibrary';
import { act } from 'react-dom/test-utils';

import Delayed from './Delayed';

describe('Delayed', () => {
  beforeAll(() => {
    jest.useFakeTimers();
  });
  afterEach(cleanup);

  const HelloWorld = () => <span>Hello World!</span>;

  it('renders children when delay is 0', () => {
    const { getByText } = render((
      <Delayed delay={0}>
        <HelloWorld />
      </Delayed>
    ));
    expect(getByText('Hello World!')).not.toBeNull();
  });

  it('renders children after delay has passed', () => {
    const { getByText, queryByText } = render((
      <Delayed delay={200}>
        <HelloWorld />
      </Delayed>
    ));
    expect(queryByText('Hello World!')).toBeNull();

    act(() => jest.advanceTimersByTime(200));

    expect(getByText('Hello World!')).not.toBeNull();
  });
});

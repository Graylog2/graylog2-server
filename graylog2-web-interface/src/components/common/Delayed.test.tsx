/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import * as React from 'react';
import { render } from 'wrappedTestingLibrary';
import { act } from 'react-dom/test-utils';

import Delayed from './Delayed';

describe('Delayed', () => {
  beforeAll(() => {
    jest.useFakeTimers();
  });

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

    // @ts-ignore
    act(() => jest.advanceTimersByTime(200));

    expect(getByText('Hello World!')).not.toBeNull();
  });
});

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
import React from 'react';
import { render } from 'wrappedTestingLibrary';
import { act } from 'react-dom/test-utils';

import Spinner from 'components/common/Spinner';

jest.useFakeTimers();

describe('<Spinner />', () => {
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

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

import ProgressBar from './ProgressBar';

describe('<ProgressBar />', () => {
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

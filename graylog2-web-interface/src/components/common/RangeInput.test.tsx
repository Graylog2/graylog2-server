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
import { render, screen, fireEvent } from 'wrappedTestingLibrary';

import RangeInput from './RangeInput';

// eslint-disable-next-line compat/compat
window.ResizeObserver = window.ResizeObserver
    || jest.fn().mockImplementation(() => ({
      disconnect: jest.fn(),
      observe: jest.fn(),
      unobserve: jest.fn(),
    }));

describe('<RangeInput />', () => {
  const SUT = (onAfterChange: (value) => void) => (
    <RangeInput label="Range"
                id="range"
                labelClassName="col-sm-3"
                wrapperClassName="col-sm-9"
                value={[1, 4]}
                min={1}
                step={1}
                max={100}
                onAfterChange={(value) => onAfterChange(value)} />
  );

  it('should render RangeInput', () => {
    render(SUT(jest.fn()));

    expect(screen.getByText(/range/i)).toBeInTheDocument();
  });

  it('should update on range slider drag', async () => {
    const updateConfig = jest.fn();

    render(SUT(updateConfig));
    const thumb2 = screen.getByText(/4/i);

    fireEvent.focus(thumb2);

    fireEvent.keyDown(thumb2, { key: 'ArrowRight' });

    expect(screen.getByText(/5/i)).toBeVisible();
  });
});

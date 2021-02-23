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
import { fireEvent, render, screen } from 'wrappedTestingLibrary';
import * as React from 'react';

import TimeRangeDisplay from './TimeRangeDisplay';

describe('TimeRangeDisplay', () => {
  it('opens the date time range picker on click', () => {
    const toggleShow = jest.fn();
    render(<TimeRangeDisplay toggleDropdownShow={toggleShow} timerange={{ type: 'relative', from: 300 }} />);

    const timeRangeDisplay = screen.getByRole('button', { name: 'Search Time Range, Opens Time Range Selector On Click' });

    fireEvent.click(timeRangeDisplay);

    expect(toggleShow).toHaveBeenCalled();
  });
});

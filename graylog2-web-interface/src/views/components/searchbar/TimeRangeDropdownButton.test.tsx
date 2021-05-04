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

import TimeRangeDropdownButton from './TimeRangeDropdownButton';

describe('TimeRangeDropdownButton', () => {
  it('button can be clicked and Popover appears', async () => {
    const toggleShow = jest.fn();
    render(<TimeRangeDropdownButton toggleShow={toggleShow}><></></TimeRangeDropdownButton>);

    const timeRangeButton = screen.getByLabelText('Open Time Range Selector');

    fireEvent.click(timeRangeButton);

    expect(toggleShow).toHaveBeenCalled();
  });
});

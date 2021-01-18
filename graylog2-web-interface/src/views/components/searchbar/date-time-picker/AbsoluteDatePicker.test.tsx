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
import { fireEvent, render, screen } from 'wrappedTestingLibrary';
import moment from 'moment';

import AbsoluteDatePicker from './AbsoluteDatePicker';

const dateTimeRange = '2020-04-08 13:22:46';

const defaultProps = {
  name: 'test-absolute-date-picker',
  dateTime: dateTimeRange,
  disabled: false,
  value: dateTimeRange,
  onChange: jest.fn(),
  startDate: new Date(dateTimeRange),
  initialDateTime: moment(dateTimeRange).toObject(),
};

describe('AbsoluteDatePicker', () => {
  it('renders with minimal props', () => {
    render(<AbsoluteDatePicker {...defaultProps} />);

    expect(screen).not.toBeNull();
  });

  it('calls onChange upon changing the input', () => {
    const { getByLabelText } = render(<AbsoluteDatePicker {...defaultProps} />);

    const input = getByLabelText('Mon Apr 20 2020');

    fireEvent.click(input);

    expect(defaultProps.onChange).toHaveBeenCalledWith('2020-04-20 13:22:46');
  });
});

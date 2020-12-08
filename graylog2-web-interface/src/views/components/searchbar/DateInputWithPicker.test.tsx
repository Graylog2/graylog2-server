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
import { fireEvent, render } from 'wrappedTestingLibrary';
import moment from 'moment-timezone';

import DateTime from 'logic/datetimes/DateTime';

import DateInputWithPicker from './DateInputWithPicker';

const dateTimeRange = '2020-04-08 13:22:46';
const initialDateTimeObject = moment(dateTimeRange).toObject();

const onChange = jest.fn();
const defaultProps = {
  value: dateTimeRange,
  initialDateTimeObject,
  onChange,
  name: 'date-picker',
};

describe('DateInputWithPicker', () => {
  beforeAll(() => { jest.clearAllMocks(); });

  it('renders with minimal props', () => {
    const { container } = render(<DateInputWithPicker {...defaultProps} />);

    expect(container).not.toBeNull();
  });

  it('calls onChange upon changing the input', () => {
    const { getByPlaceholderText } = render(<DateInputWithPicker {...defaultProps} />);

    const input = getByPlaceholderText(DateTime.Formats.DATETIME);

    fireEvent.change(input, { target: { value: 'something' } });

    expect(defaultProps.onChange).toHaveBeenCalled();
  });

  it('pressing magic wand inserts current date', () => {
    const output = moment().format(DateTime.Formats.TIMESTAMP);
    defaultProps.onChange.mockReturnValueOnce(output);
    const { getByTitle } = render(<DateInputWithPicker {...defaultProps} />);

    const insertCurrentDate = getByTitle('Insert current date');

    fireEvent.click(insertCurrentDate);

    expect(defaultProps.onChange).toHaveReturnedWith(output);
  });
});

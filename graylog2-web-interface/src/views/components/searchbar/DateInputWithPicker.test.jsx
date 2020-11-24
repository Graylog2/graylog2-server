// @flow strict
import * as React from 'react';
import { fireEvent, render } from 'wrappedTestingLibrary';
import moment from 'moment-timezone';

import DateTime from 'logic/datetimes/DateTime';

import DateInputWithPicker from './DateInputWithPicker';

const dateTimeRange = '2020-04-08 13:22:46';
const initialDateTimeObject = moment(dateTimeRange).toObject();

describe('DateInputWithPicker', () => {
  beforeAll(() => { jest.clearAllMocks(); });

  it('renders with minimal props', () => {
    const { container } = render(<DateInputWithPicker value={dateTimeRange}
                                                      initialDateTimeObject={initialDateTimeObject}
                                                      onChange={() => {}}
                                                      name="date-picker" />);

    expect(container).not.toBeNull();
  });

  it('calls onChange upon changing the input', () => {
    const onChange = jest.fn();
    const { getByPlaceholderText } = render(<DateInputWithPicker value={dateTimeRange}
                                                                 initialDateTimeObject={initialDateTimeObject}
                                                                 onChange={onChange}
                                                                 name="date-picker" />);

    const input = getByPlaceholderText(DateTime.Formats.DATETIME);

    fireEvent.change(input, { target: { value: 'something' } });

    expect(onChange).toHaveBeenCalled();
  });

  it('pressing magic wand inserts current date', () => {
    const output = moment().format(DateTime.Formats.TIMESTAMP);
    const onChange = jest.fn(() => output);
    const { getByTitle } = render(<DateInputWithPicker value={dateTimeRange}
                                                       initialDateTimeObject={initialDateTimeObject}
                                                       onChange={onChange}
                                                       name="date-picker" />);

    const insertCurrentDate = getByTitle('Insert current date');

    fireEvent.click(insertCurrentDate);

    expect(onChange).toHaveReturnedWith(output);
  });
});

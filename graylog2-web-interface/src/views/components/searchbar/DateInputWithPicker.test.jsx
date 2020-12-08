// @flow strict
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

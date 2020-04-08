// @flow strict
import * as React from 'react';
import { cleanup, fireEvent, render, wait } from 'wrappedTestingLibrary';
import moment from 'moment-timezone';

import asMock from 'helpers/mocking/AsMock';
import DateTime from 'logic/datetimes/DateTime';
import DateInputWithPicker from './DateInputWithPicker';

describe('DateInputWithPicker', () => {
  afterEach(cleanup);

  beforeAll(() => { jest.clearAllMocks(); });

  it('renders with minimal props', () => {
    const { container } = render(<DateInputWithPicker value="2020-04-08 13:22:46" onChange={() => {}} name="date-picker" />);
    expect(container).not.toBeNull();
  });

  it('shows date picker when focussing input', async () => {
    const { getByPlaceholderText, getByText } = render((
      <DateInputWithPicker value="2020-04-08 13:22:46"
                           onChange={() => {}}
                           title="Pick start date"
                           name="date-picker" />
    ));

    const input = getByPlaceholderText(DateTime.Formats.DATETIME);

    fireEvent.click(input);

    await wait(() => getByText('Pick start date'));
  });

  it('calls onChange upon changing the input', () => {
    const onChange = jest.fn();
    const { getByPlaceholderText } = render(<DateInputWithPicker value="2020-04-08 13:22:46" onChange={onChange} name="date-picker" />);

    const input = getByPlaceholderText(DateTime.Formats.DATETIME);

    fireEvent.change(input, { target: { value: 'something' } });

    expect(onChange).toHaveBeenCalled();
  });

  it('pressing magic wand inserts current date', () => {
    DateTime.now = jest.fn(DateTime.now);
    asMock(DateTime.now).mockReturnValue(moment('2020-04-08T17:18:36.315Z').tz('utc'));
    const onChange = jest.fn();
    const { getByTitle } = render(<DateInputWithPicker value="2020-04-04 13:22:46" onChange={onChange} name="date-picker" />);

    const insertCurrentDate = getByTitle('Insert current date');

    fireEvent.click(insertCurrentDate);

    expect(onChange).toHaveBeenCalledWith({ target: { name: 'date-picker', value: '2020-04-08 19:18:36.315' } });
  });
});

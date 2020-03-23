// @flow strict
import * as React from 'react';
import { cleanup, fireEvent, render } from 'wrappedTestingLibrary';

import AbsoluteTimeRangeSelector from './AbsoluteTimeRangeSelector';

describe('AbsoluteTimeRangeSelector', () => {
  afterEach(cleanup);
  it('does not try to parse an empty date in from field', () => {
    const onChange = jest.fn();
    const { getByDisplayValue } = render((
      <AbsoluteTimeRangeSelector from="2020-01-16 10:04:30.329"
                                 to="020-01-16 12:04:30.329"
                                 onChange={onChange}
                                 onSubmit={() => {}} />
    ));
    const fromDate = getByDisplayValue('2020-01-16 10:04:30.329');

    fireEvent.change(fromDate, { target: { value: '' } });
    fireEvent.blur(fromDate);

    expect(onChange).not.toHaveBeenCalled();
  });
  it('does not try to parse an empty date in to field', () => {
    const onChange = jest.fn();
    const { getByDisplayValue } = render((
      <AbsoluteTimeRangeSelector from="2020-01-16 10:04:30.329"
                                 to="2020-01-16 12:04:30.329"
                                 onChange={onChange}
                                 onSubmit={() => {}} />
    ));
    const toDate = getByDisplayValue('2020-01-16 12:04:30.329');

    fireEvent.change(toDate, { target: { value: '' } });
    fireEvent.blur(toDate);

    expect(onChange).not.toHaveBeenCalled();
  });
});

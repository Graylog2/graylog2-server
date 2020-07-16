// @flow strict
import * as React from 'react';
import { asElement, cleanup, fireEvent, render, waitFor } from 'wrappedTestingLibrary';
import { act } from 'react-dom/test-utils';

import SearchBarForm from './SearchBarForm';
import AbsoluteTimeRangeSelector from './AbsoluteTimeRangeSelector';

const changeInput = async (input, value) => {
  const { name } = asElement(input, HTMLInputElement);

  await act(async () => { fireEvent.change(input, { target: { value, name } }); });
};

describe('SearchBarForm', () => {
  afterEach(cleanup);

  describe('with AbsoluteTimeRangeSelector', () => {
    it('validates if timerange "from" date is after "to" date', async () => {
      const initialValues = {
        timerange: { type: 'absolute', from: '2020-01-16 10:04:30.329', to: '2020-01-17 10:04:30.329' },
        queryString: '*',
        streams: [],
      };
      const { getByDisplayValue, queryByText } = render(
        <SearchBarForm onSubmit={() => {}}
                       initialValues={initialValues}>
          <AbsoluteTimeRangeSelector />
        </SearchBarForm>,
      );
      const fromDate = getByDisplayValue('2020-01-16 10:04:30.329');

      await changeInput(fromDate, '2020-01-18 10:04:30.329');

      await waitFor(() => expect(queryByText('Start date must be before end date')).not.toBeNull());
    });
  });
});

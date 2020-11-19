// @flow strict
import * as React from 'react';
import { render } from 'wrappedTestingLibrary';

import SearchBarForm from './SearchBarForm';
import AbsoluteTimeRangeSelector from './date-time-picker/AbsoluteTimeRangeSelector';

describe('SearchBarForm', () => {
  describe('with AbsoluteTimeRangeSelector', () => {
    it('renders', () => {
      const initialValues = {
        timerange: { type: 'absolute', from: '2020-01-16 10:04:30.329', to: '2020-01-17 10:04:30.329' },
        queryString: '*',
        streams: [],
      };
      const originalTimeRange = { from: '1955-05-10 06:15:00.000', to: '1985-25-11 08:18:00.000' };

      const { asFragment } = render(
        <SearchBarForm onSubmit={() => {}}
                       initialValues={initialValues}>
          <AbsoluteTimeRangeSelector originalTimeRange={originalTimeRange} currentTimerange={initialValues.timerange} />
        </SearchBarForm>,
      );

      expect(asFragment()).toMatchSnapshot();
    });
  });
});

// @flow strict
import * as React from 'react';
import { render, screen } from 'wrappedTestingLibrary';

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

      render(
        <SearchBarForm onSubmit={() => {}}
                       initialValues={initialValues}>
          <AbsoluteTimeRangeSelector />
        </SearchBarForm>,
      );

      expect(screen).toMatchSnapshot();
    });
  });
});

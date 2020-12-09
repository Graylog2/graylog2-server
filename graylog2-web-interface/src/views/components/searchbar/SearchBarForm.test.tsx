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
import { render } from 'wrappedTestingLibrary';

import { AbsoluteTimeRange } from 'views/logic/queries/Query';

import SearchBarForm, { Values } from './SearchBarForm';
import AbsoluteTimeRangeSelector from './date-time-picker/AbsoluteTimeRangeSelector';

describe('SearchBarForm', () => {
  describe('with AbsoluteTimeRangeSelector', () => {
    it('renders', () => {
      const initialValues: Values & { timerange: AbsoluteTimeRange } = {
        limitDuration: 0,
        timerange: { type: 'absolute', from: '2020-01-16 10:04:30.329', to: '2020-01-17 10:04:30.329' },
        queryString: '*',
        streams: [],
      };
      const originalTimeRange = { type: 'absolute', from: '1955-05-10 06:15:00.000', to: '1985-25-11 08:18:00.000' } as const;

      const { asFragment } = render(
        <SearchBarForm onSubmit={() => {}}
                       initialValues={initialValues}>
          <AbsoluteTimeRangeSelector originalTimeRange={originalTimeRange} currentTimeRange={initialValues.timerange} />
        </SearchBarForm>,
      );

      expect(asFragment()).toMatchSnapshot();
    });
  });
});

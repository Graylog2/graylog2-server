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
import { asElement, fireEvent, render, waitFor } from 'wrappedTestingLibrary';
import { act } from 'react-dom/test-utils';

import SearchBarForm, { Values } from './SearchBarForm';
import AbsoluteTimeRangeSelector from './AbsoluteTimeRangeSelector';

const changeInput = async (input, value) => {
  const { name } = asElement(input, HTMLInputElement);

  await act(async () => { fireEvent.change(input, { target: { value, name } }); });
};

describe('SearchBarForm', () => {
  describe('with AbsoluteTimeRangeSelector', () => {
    it('validates if timerange "from" date is after "to" date', async () => {
      const initialValues: Values = {
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

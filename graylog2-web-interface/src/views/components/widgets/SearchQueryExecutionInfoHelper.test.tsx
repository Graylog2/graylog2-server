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
import * as Immutable from 'immutable';
import { render, screen, waitFor } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';

import TestStoreProvider from 'views/test/TestStoreProvider';
import useViewsPlugin from 'views/test/testViewsPlugin';
import useViewsSelector from 'views/stores/useViewsSelector';
import { asMock } from 'helpers/mocking';

import SearchQueryExecutionInfoHelper from './SearchQueryExecutionInfoHelper';

jest.mock('views/stores/useViewsSelector');

const currentWidgetMapping = Immutable.Set(['search-type-id']);

const SimpleSearchQueryExecutionInfoHelper = () => (
  <TestStoreProvider>
    <SearchQueryExecutionInfoHelper currentWidgetMapping={currentWidgetMapping}>
      <span>Time Range</span>
    </SearchQueryExecutionInfoHelper>
    <button type="button">Outside element</button>
  </TestStoreProvider>
);

describe('SearchQueryExecutionInfoHelper', () => {
  useViewsPlugin();

  beforeEach(() => {
    asMock(useViewsSelector).mockReturnValue({
      duration: 42,
      timestamp: '2021-04-26T14:32:48.000Z',
      searchTypes: {
        'search-type-id': {
          type: 'pivot',
          effective_timerange: {
            type: 'absolute',
            from: '2021-04-26T12:32:48.000Z',
            to: '2021-04-26T14:32:48.000Z',
          },
          total: 1000,
        },
      },
    });
  });

  it('should open the popover when clicking the trigger', async () => {
    render(<SimpleSearchQueryExecutionInfoHelper />);

    await userEvent.click(screen.getByText('Time Range'));

    await screen.findByText('Execution Info');
  });

  it('should close the popover when clicking outside', async () => {
    render(<SimpleSearchQueryExecutionInfoHelper />);

    await userEvent.click(screen.getByText('Time Range'));
    await screen.findByText('Execution Info');

    await userEvent.click(screen.getByText('Outside element'));

    await waitFor(() => {
      expect(screen.queryByText('Execution Info')).not.toBeInTheDocument();
    });
  });
});

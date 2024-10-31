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
import { render, fireEvent, waitFor, screen } from 'wrappedTestingLibrary';

import GlobalOverride from 'views/logic/search/GlobalOverride';
import Widget from 'views/logic/widgets/Widget';
import mockComponent from 'helpers/mocking/MockComponent';
import useViewsPlugin from 'views/test/testViewsPlugin';
import TestStoreProvider from 'views/test/TestStoreProvider';
import { asMock } from 'helpers/mocking';
import useGlobalOverride from 'views/hooks/useGlobalOverride';
import { setGlobalOverrideTimerange, setGlobalOverrideQuery, execute } from 'views/logic/slices/searchExecutionSlice';

import WidgetQueryControls from './WidgetQueryControls';
import WidgetContext from './contexts/WidgetContext';

jest.mock('views/components/searchbar/queryvalidation/QueryValidation', () => mockComponent('QueryValidation'));
jest.mock('views/components/searchbar/queryvalidation/QueryValidation', () => mockComponent('QueryValidation'));

jest.mock('views/components/searchbar/queryinput/QueryInput');
jest.mock('views/components/searchbar/queryinput/BasicQueryInput');
jest.mock('views/logic/fieldtypes/useFieldTypes');
jest.mock('views/hooks/useGlobalOverride');

jest.mock('views/logic/slices/searchExecutionSlice', () => ({
  ...jest.requireActual('views/logic/slices/searchExecutionSlice'),
  setGlobalOverrideTimerange: jest.fn(() => async () => {}),
  setGlobalOverrideQuery: jest.fn(() => async () => {}),
  execute: jest.fn(() => async () => {}),
}));

describe('WidgetQueryControls', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    asMock(useGlobalOverride).mockReturnValue(GlobalOverride.empty());
  });

  useViewsPlugin();

  const config = {
    relative_timerange_options: { P1D: 'Search in last day', PT0S: 'Search in all messages' },
    query_time_range_limit: 'PT0S',
  };

  const defaultProps = {
    availableStreams: [],
    config,
  };

  const emptyGlobalOverride = GlobalOverride.empty();
  const globalOverrideWithQuery = GlobalOverride.create(undefined, { type: 'elasticsearch', query_string: 'source:foo' });
  const globalOverrideWithTimeRange = GlobalOverride.create({ type: 'absolute', from: '2020-01-01T10:00:00.850Z', to: '2020-01-02T10:00:00.000Z' });
  const globalOverrideWithQueryAndTimeRange = GlobalOverride.create(
    { type: 'absolute', from: '2020-01-01T10:00:00.850Z', to: '2020-01-02T10:00:00.000Z' },
    { type: 'elasticsearch', query_string: 'source:foo' },
  );
  const widget = Widget.builder()
    .id('deadbeef')
    .type('dummy')
    .config({})
    .build();

  const Wrapper = ({ children }: { children: React.ReactNode }) => (
    <TestStoreProvider>
      <WidgetContext.Provider value={widget}>
        {children}
      </WidgetContext.Provider>
    </TestStoreProvider>
  );

  const renderSUT = (props = {}) => render(
    <Wrapper>
      <WidgetQueryControls {...defaultProps}
                           {...props} />
    </Wrapper>,
  );

  describe('displays if global override is set', () => {
    const resetTimeRangeButtonTitle = /reset global override/i;
    const resetQueryButtonTitle = /reset global filter/i;
    const timeRangeOverrideInfo = '2020-01-01 11:00:00.850 - 2020-01-02 11:00:00.000';
    const queryOverrideInfo = globalOverrideWithQuery.query.query_string;

    it('shows preview of global override time range', async () => {
      asMock(useGlobalOverride).mockReturnValue(globalOverrideWithTimeRange);

      renderSUT();

      await screen.findByRole('button', { name: resetTimeRangeButtonTitle });
      await screen.findByText(timeRangeOverrideInfo);
    });

    it('shows preview of global override query', async () => {
      asMock(useGlobalOverride).mockReturnValue(globalOverrideWithQuery);

      renderSUT();

      await screen.findByRole('button', { name: resetQueryButtonTitle });
      await screen.findByText(queryOverrideInfo);
    });

    it('does not show any indicator if global override is not set', async () => {
      renderSUT();

      expect(screen.queryByRole('button', { name: resetTimeRangeButtonTitle })).toBeNull();
      expect(screen.queryByRole('button', { name: resetQueryButtonTitle })).toBeNull();
    });

    it('does not show global override query indicator if global override query is an object with an empty query string', async () => {
      asMock(useGlobalOverride).mockReturnValue(GlobalOverride.create(undefined, { type: 'elasticsearch', query_string: '' }));
      renderSUT();

      expect(screen.queryByRole('button', { name: resetQueryButtonTitle })).toBeNull();
    });

    it('triggers resetting global override when reset time range override button is clicked', async () => {
      asMock(useGlobalOverride).mockReturnValue(globalOverrideWithTimeRange);

      renderSUT();

      const resetTimeRangeOverrideButton = await screen.findByRole('button', { name: resetTimeRangeButtonTitle });
      fireEvent.click(resetTimeRangeOverrideButton);

      expect(setGlobalOverrideTimerange).toHaveBeenCalledWith(undefined);
    });

    it('triggers resetting global override and query validation when reset query filter button is clicked', async () => {
      asMock(useGlobalOverride).mockReturnValue(globalOverrideWithQuery);

      renderSUT();

      const resetQueryFilterButton = await screen.findByRole('button', { name: resetQueryButtonTitle });
      fireEvent.click(resetQueryFilterButton);

      expect(setGlobalOverrideQuery).toHaveBeenCalledWith(undefined);
    });

    it('executes search when reset time range override button is clicked', async () => {
      asMock(useGlobalOverride).mockReturnValue(globalOverrideWithTimeRange);

      renderSUT();

      const resetTimeRangeOverrideButton = await screen.findByRole('button', { name: resetTimeRangeButtonTitle });
      fireEvent.click(resetTimeRangeOverrideButton);
      await waitFor(() => expect(execute).toHaveBeenCalled());
    });

    it('executes search when reset query filter button is clicked', async () => {
      asMock(useGlobalOverride).mockReturnValue(globalOverrideWithQuery);

      renderSUT();

      const resetQueryFilterButton = await screen.findByRole('button', { name: resetQueryButtonTitle });
      fireEvent.click(resetQueryFilterButton);
      await waitFor(() => expect(execute).toHaveBeenCalled());
    });

    it('emptying `globalOverride` prop removes notifications', async () => {
      asMock(useGlobalOverride).mockReturnValue(globalOverrideWithQueryAndTimeRange);

      const { rerender } = renderSUT();

      await screen.findByText(queryOverrideInfo);
      await screen.findByText(timeRangeOverrideInfo);

      asMock(useGlobalOverride).mockReturnValue(emptyGlobalOverride);

      rerender(
        <Wrapper>
          <WidgetQueryControls {...defaultProps} />
        </Wrapper>,
      );

      expect(screen.queryByText(queryOverrideInfo)).toBeNull();
      expect(screen.queryByText(timeRangeOverrideInfo)).toBeNull();
    });
  });
});

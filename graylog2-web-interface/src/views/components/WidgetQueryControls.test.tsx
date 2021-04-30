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
import WrappingContainer from 'WrappingContainer';
import MockStore from 'helpers/mocking/StoreMock';

import { GlobalOverrideActions } from 'views/stores/GlobalOverrideStore';
import SearchActions from 'views/actions/SearchActions';
import GlobalOverride from 'views/logic/search/GlobalOverride';
import Widget from 'views/logic/widgets/Widget';

import WidgetQueryControls from './WidgetQueryControls';
import WidgetContext from './contexts/WidgetContext';

jest.mock('views/stores/WidgetStore', () => ({
  WidgetActions: {
    update: jest.fn(),
  },
}));

jest.mock('views/stores/GlobalOverrideStore', () => ({
  GlobalOverrideActions: {
    resetTimeRange: jest.fn(() => Promise.resolve()),
    resetQuery: jest.fn(() => Promise.resolve()),
  },
}));

jest.mock('views/actions/SearchActions', () => ({
  refresh: jest.fn(() => Promise.resolve()),
}));

jest.mock('stores/connect', () => {
  const originalModule = jest.requireActual('stores/connect');

  return {
    __esModule: true,
    ...originalModule,
    default: (x) => x,
  };
});

jest.mock('moment', () => {
  const mockMoment = jest.requireActual('moment');

  return Object.assign(() => mockMoment('2019-10-10T12:26:31.146Z'), mockMoment);
});

jest.mock('views/components/searchbar/QueryInput', () => ({ value = '' }: { value: string }) => <span>{value}</span>);

jest.mock('views/stores/SearchConfigStore', () => ({
  SearchConfigStore: MockStore(['getInitialState', () => ({
    searchesClusterConfig: {
      relative_timerange_options: { P1D: 'Search in last day', PT0S: 'Search in all messages' },
      query_time_range_limit: 'PT0S',
    },
  })]),
}));

describe('WidgetQueryControls', () => {
  beforeEach(() => { jest.clearAllMocks(); });

  const config = {
    relative_timerange_options: { P1D: 'Search in last day', PT0S: 'Search in all messages' },
    query_time_range_limit: 'PT0S',
  };

  const defaultProps = {
    availableStreams: [],
    config,
  };

  const emptyGlobalOverride = {};
  const globalOverrideWithQuery = { query: { type: 'elasticsearch', query_string: 'source:foo' } };
  const globalOverrideWithTimeRange = { timerange: { type: 'absolute', from: '2020-01-01T10:00:00.850Z', to: '2020-01-02T10:00:00.000Z' } };
  const globalOverrideWithQueryAndTimeRange = { ...globalOverrideWithQuery, ...globalOverrideWithTimeRange };
  const widget = Widget.builder()
    .id('deadbeef')
    .type('dummy')
    .config({})
    .build();

  const Wrapper = ({ children }: { children: React.ReactNode }) => (
    <WrappingContainer>
      <WidgetContext.Provider value={widget}>
        {children}
      </WidgetContext.Provider>
    </WrappingContainer>
  );

  const renderSUT = (props = {}) => render(
    <Wrapper>
      <WidgetQueryControls {...defaultProps}
                           {...props} />
    </Wrapper>,
  );

  it('should do something', () => {
    const { container } = renderSUT();

    expect(container).not.toBeNull();
  });

  describe('displays if global override is set', () => {
    const resetTimeRangeButtonTitle = /reset global override/i;
    const resetQueryButtonTitle = /reset global filter/i;
    const timeRangeOverrideInfo = '2020-01-01T10:00:00.850Z - 2020-01-02T10:00:00.000Z';
    const queryOverrideInfo = globalOverrideWithQuery.query.query_string;

    it('shows preview of global override time range', async () => {
      renderSUT({ globalOverride: globalOverrideWithTimeRange });

      await screen.findByRole('button', { name: resetTimeRangeButtonTitle });
      await screen.findByText(timeRangeOverrideInfo);
    });

    it('shows preview of global override query', async () => {
      renderSUT({ globalOverride: globalOverrideWithQuery });

      await screen.findByRole('button', { name: resetQueryButtonTitle });
      await screen.findByText(queryOverrideInfo);
    });

    it('does not show any indicator if global override is not set', async () => {
      renderSUT({ globalOverride: emptyGlobalOverride });

      expect(screen.queryByRole('button', { name: resetTimeRangeButtonTitle })).toBeNull();
      expect(screen.queryByRole('button', { name: resetTimeRangeButtonTitle })).toBeNull();
    });

    it('triggers resetting global override when reset time range override button is clicked', async () => {
      renderSUT({ globalOverride: globalOverrideWithTimeRange });
      const resetTimeRangeOverrideButton = await screen.findByRole('button', { name: resetTimeRangeButtonTitle });
      fireEvent.click(resetTimeRangeOverrideButton);

      expect(GlobalOverrideActions.resetTimeRange).toHaveBeenCalled();
    });

    it('triggers resetting global override when reset query filter button is clicked', async () => {
      renderSUT({ globalOverride: globalOverrideWithQuery });
      const resetQueryFilterButton = await screen.findByRole('button', { name: resetQueryButtonTitle });
      fireEvent.click(resetQueryFilterButton);

      expect(GlobalOverrideActions.resetQuery).toHaveBeenCalled();
    });

    it('executes search when reset time range override button is clicked', async () => {
      renderSUT({ globalOverride: globalOverrideWithTimeRange });
      const resetTimeRangeOverrideButton = await screen.findByRole('button', { name: resetTimeRangeButtonTitle });
      fireEvent.click(resetTimeRangeOverrideButton);
      await waitFor(() => expect(SearchActions.refresh).toHaveBeenCalled());
    });

    it('executes search when reset query filter button is clicked', async () => {
      renderSUT({ globalOverride: globalOverrideWithQuery });
      const resetQueryFilterButton = await screen.findByRole('button', { name: resetQueryButtonTitle });
      fireEvent.click(resetQueryFilterButton);
      await waitFor(() => expect(SearchActions.refresh).toHaveBeenCalled());
    });

    it('emptying `globalOverride` prop removes notifications', async () => {
      const { rerender } = renderSUT({ globalOverride: globalOverrideWithQueryAndTimeRange });

      await screen.findByText(queryOverrideInfo);
      await screen.findByText(timeRangeOverrideInfo);

      rerender(
        <Wrapper>
          <WidgetQueryControls {...defaultProps} globalOverride={emptyGlobalOverride as GlobalOverride} />
        </Wrapper>,
      );

      expect(screen.queryByText(queryOverrideInfo)).toBeNull();
      expect(screen.queryByText(timeRangeOverrideInfo)).toBeNull();
    });
  });
});

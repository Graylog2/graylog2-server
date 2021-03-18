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
import { render, fireEvent, waitFor } from 'wrappedTestingLibrary';
import WrappingContainer from 'WrappingContainer';
import MockStore from 'helpers/mocking/StoreMock';

import { GlobalOverrideActions } from 'views/stores/GlobalOverrideStore';
import SearchActions from 'views/actions/SearchActions';
import { DEFAULT_TIMERANGE } from 'views/Constants';
import GlobalOverride from 'views/logic/search/GlobalOverride';
import Widget from 'views/logic/widgets/Widget';

import WidgetQueryControls from './WidgetQueryControls';
import SearchBarForm from './searchbar/SearchBarForm';
import WidgetContext from './contexts/WidgetContext';

jest.mock('views/stores/WidgetStore', () => ({
  WidgetActions: {
    update: jest.fn(),
  },
}));

jest.mock('views/stores/GlobalOverrideStore', () => ({
  GlobalOverrideActions: {
    reset: jest.fn(() => Promise.resolve()),
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

jest.mock('views/components/searchbar/QueryInput', () => () => <span>Query Input</span>);

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
    const indicatorText = 'These controls are disabled, because a filter is applied to all widgets.';

    it('shows an indicator if global override is set', async () => {
      const { findByText, findByTestId } = renderSUT({ globalOverride: globalOverrideWithQuery });

      await findByText(indicatorText);
      await findByTestId('reset-filter');
    });

    it('does not show an indicator if global override is not set', async () => {
      const { queryByText } = renderSUT({ globalOverride: emptyGlobalOverride });

      expect(queryByText(indicatorText)).toBeNull();
    });

    it('triggers resetting the global override store when reset filter button is clicked', async () => {
      const { findByTestId } = renderSUT({ globalOverride: globalOverrideWithQuery });
      const resetFilterButton = await findByTestId('reset-filter');
      fireEvent.click(resetFilterButton);

      expect(GlobalOverrideActions.reset).toHaveBeenCalled();
    });

    it('executes search when reset filter button is clicked', async () => {
      const { findByTestId } = renderSUT({ globalOverride: globalOverrideWithQuery });
      const resetFilterButton = await findByTestId('reset-filter');
      fireEvent.click(resetFilterButton);
      await waitFor(() => expect(SearchActions.refresh).toHaveBeenCalled());
    });

    it('emptying `globalOverride` prop removes notification', async () => {
      const { findByText, rerender, queryByText } = renderSUT({ globalOverride: globalOverrideWithQuery });

      await findByText(indicatorText);

      rerender(
        <Wrapper>
          <WidgetQueryControls {...defaultProps} globalOverride={emptyGlobalOverride as GlobalOverride} />
        </Wrapper>,
      );

      expect(queryByText(indicatorText)).toBeNull();
    });
  });
});

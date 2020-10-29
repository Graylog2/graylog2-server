// @flow strict
import * as React from 'react';
import { render, fireEvent, waitFor } from 'wrappedTestingLibrary';
import WrappingContainer from 'WrappingContainer';

import { GlobalOverrideActions } from 'views/stores/GlobalOverrideStore';
import SearchActions from 'views/actions/SearchActions';
import { DEFAULT_TIMERANGE } from 'views/Constants';

import WidgetQueryControls from './WidgetQueryControls';
import SearchBarForm from './searchbar/SearchBarForm';

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

jest.mock('stores/connect', () => (x) => x);

jest.mock('moment', () => {
  const mockMoment = jest.requireActual('moment');

  return Object.assign(() => mockMoment('2019-10-10T12:26:31.146Z'), mockMoment);
});

jest.mock('views/components/searchbar/QueryInput', () => () => <span>Query Input</span>);

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

  const Wrapper = ({ children }: { children: React.Node }) => (
    <WrappingContainer>
      <SearchBarForm initialValues={{ timerange: DEFAULT_TIMERANGE, queryString: '', streams: [] }} onSubmit={() => {}}>
        {children}
      </SearchBarForm>
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
          <WidgetQueryControls {...defaultProps} globalOverride={emptyGlobalOverride} />
        </Wrapper>,
      );

      expect(queryByText(indicatorText)).toBeNull();
    });

    // it('disables timerange controls when global override is present', () => {
    //   const { getByDisplayValue } = renderSUT({ globalOverride: globalOverrideWithQuery });
    //   const timeRangeSelect = getByDisplayValue('Search in last day');
    //
    //   expect(timeRangeSelect).toBeDisabled();
    // });
  });
});

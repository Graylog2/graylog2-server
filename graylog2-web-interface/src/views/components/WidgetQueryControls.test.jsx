// @flow strict
import * as React from 'react';
import { asElement, render, waitForElement, cleanup, fireEvent, waitFor } from 'wrappedTestingLibrary';
import selectEvent from 'react-select-event';
import WrappingContainer from 'WrappingContainer';

import { GlobalOverrideActions } from 'views/stores/GlobalOverrideStore';
import SearchActions from 'views/actions/SearchActions';
import Widget from 'views/logic/widgets/Widget';

import WidgetQueryControls from './WidgetQueryControls';

import { WidgetActions } from '../stores/WidgetStore';

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

  afterEach(cleanup);

  const config = {
    relative_timerange_options: { P1D: 'Search in last day', PT0S: 'Search in all messages' },
    query_time_range_limit: 'PT0S',
  };

  const defaultProps = {
    widget: Widget.builder()
      .id('deadbeef')
      .type('foo')
      .build(),
    availableStreams: [],
    config,
  };

  const emptyGlobalOverride = {};
  const globalOverrideWithQuery = { query: { type: 'elasticsearch', query_string: 'source:foo' } };

  const renderSUT = (props = {}) => render(
    <WrappingContainer>
      <WidgetQueryControls {...defaultProps}
                           {...props} />
    </WrappingContainer>,
  );

  it('should do something', () => {
    const { container } = renderSUT();

    expect(container).toMatchSnapshot();
  });

  describe('displays if global override is set', () => {
    const indicatorText = 'These controls are disabled, because a filter is applied to all widgets.';

    it('shows an indicator if global override is set', async () => {
      const { getByText, getByTestId } = renderSUT({ globalOverride: globalOverrideWithQuery });

      await waitForElement(() => getByText(indicatorText));
      await waitForElement(() => getByTestId('reset-filter'));
    });

    it('does not show an indicator if global override is not set', async () => {
      const { queryByText } = renderSUT({ globalOverride: emptyGlobalOverride });

      expect(queryByText(indicatorText)).toBeNull();
    });

    it('triggers resetting the global override store when reset filter button is clicked', async () => {
      const { getByTestId } = renderSUT({ globalOverride: globalOverrideWithQuery });
      const resetFilterButton = await waitForElement(() => getByTestId('reset-filter'));

      fireEvent.click(resetFilterButton);

      expect(GlobalOverrideActions.reset).toHaveBeenCalled();
    });

    it('executes search when reset filter button is clicked', async () => {
      const { getByTestId } = renderSUT({ globalOverride: globalOverrideWithQuery });
      const resetFilterButton = await waitForElement(() => getByTestId('reset-filter'));

      fireEvent.click(resetFilterButton);
      await waitFor(() => expect(SearchActions.refresh).toHaveBeenCalled());
    });

    it('emptying `globalOverride` prop removes notification', async () => {
      const { getByText, rerender, queryByText } = renderSUT({ globalOverride: globalOverrideWithQuery });

      await waitForElement(() => getByText(indicatorText));

      rerender(
        <WrappingContainer>
          <WidgetQueryControls {...defaultProps} globalOverride={emptyGlobalOverride} />
        </WrappingContainer>,
      );

      expect(queryByText(indicatorText)).toBeNull();
    });

    it('disables timerange controls when global override is present', () => {
      const { getByDisplayValue } = renderSUT({ globalOverride: globalOverrideWithQuery });
      const timeRangeSelect = getByDisplayValue('Search in last day');

      expect(timeRangeSelect).toBeDisabled();
    });
  });

  it('changes the widget\'s timerange when time range input is used', async () => {
    const { getByDisplayValue, getByText, getByTitle } = renderSUT();
    const timeRangeSelect = getByDisplayValue('Search in last day');

    expect(timeRangeSelect).not.toBeNull();

    const optionForAllMessages = asElement(getByText('Search in all messages'), HTMLOptionElement);

    fireEvent.change(timeRangeSelect, { target: { value: optionForAllMessages.value } });

    const searchButton = getByTitle(/Perform search/);

    fireEvent.click(searchButton);

    await waitFor(() => expect(WidgetActions.update).toHaveBeenCalledWith('deadbeef', expect.objectContaining({
      timerange: { type: 'relative', range: 0 },
    })));
  });

  it('changes the widget\'s timerange type when switching to absolute time range', async () => {
    const { getByText, getByTitle } = renderSUT();
    const absoluteTimeRangeSelect = getByText('Absolute');

    expect(absoluteTimeRangeSelect).not.toBeNull();

    fireEvent.click(absoluteTimeRangeSelect);

    const searchButton = getByTitle(/Perform search/);

    fireEvent.click(searchButton);

    await waitFor(() => expect(WidgetActions.update)
      .toHaveBeenLastCalledWith('deadbeef', expect.objectContaining({
        timerange: {
          type: 'absolute',
          from: '2019-10-10T12:21:31.146Z',
          to: '2019-10-10T12:26:31.146Z',
        },
      })));
  });

  it('changes the widget\'s streams when using stream filter', async () => {
    const { container, getByTitle } = renderSUT({
      availableStreams: [
        { key: 'PFLog', value: '5c2e27d6ba33a9681ad62775' },
        { key: 'DNS Logs', value: '5d2d9649e117dc4df84cf83c' },
      ],
    });
    const streamFilter = container.querySelector('div[data-testid="streams-filter"] > div');

    expect(streamFilter).not.toBeNull();

    // Flow is not parsing the jest assertion before
    if (streamFilter) {
      await selectEvent.select(streamFilter, 'PFLog');
    }

    const searchButton = getByTitle(/Perform search/);

    fireEvent.click(searchButton);

    await waitFor(() => expect(WidgetActions.update).toHaveBeenCalledWith('deadbeef', expect.objectContaining({
      streams: ['5c2e27d6ba33a9681ad62775'],
    })));
  });
});

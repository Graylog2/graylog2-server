// @flow strict
import * as React from 'react';
import { render, waitForElement, cleanup, fireEvent, wait } from 'wrappedTestingLibrary';
import selectEvent from 'react-select-event';
import '@testing-library/jest-dom/extend-expect';

import { GlobalOverrideActions } from 'views/stores/GlobalOverrideStore';
import SearchActions from 'views/actions/SearchActions';
import Widget from 'views/logic/widgets/Widget';
import GraylogThemeProvider from 'theme/GraylogThemeProvider';
import WidgetQueryControls from './WidgetQueryControls';
import { WidgetActions } from '../stores/WidgetStore';


jest.mock('views/stores/WidgetStore', () => ({
  WidgetActions: {
    timerange: jest.fn(),
    streams: jest.fn(),
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
jest.mock('stores/connect', () => x => x);

jest.mock('moment', () => {
  const mockMoment = jest.requireActual('moment');
  return Object.assign(() => mockMoment('2019-10-10T12:26:31.146Z'), mockMoment);
});

describe('WidgetQueryControls', () => {
  afterEach(cleanup);

  const config = {
    relative_timerange_options: { PT1D: 'Search in last day', PT0S: 'Search in all messages' },
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
    <GraylogThemeProvider>
      <WidgetQueryControls {...defaultProps}
                           {...props} />
    </GraylogThemeProvider>,
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
      await wait(() => expect(SearchActions.refresh).toHaveBeenCalled());
    });

    it('emptying `globalOverride` prop removes notification', async () => {
      const { getByText, rerender, queryByText } = renderSUT({ globalOverride: globalOverrideWithQuery });
      await waitForElement(() => getByText(indicatorText));

      rerender(
        <GraylogThemeProvider>
          <WidgetQueryControls {...defaultProps} globalOverride={emptyGlobalOverride} />
        </GraylogThemeProvider>,
      );

      expect(queryByText(indicatorText)).toBeNull();
    });

    it('disables timerange controls when global override is present', () => {
      const { getByDisplayValue } = renderSUT({ globalOverride: globalOverrideWithQuery });
      const timeRangeSelect = getByDisplayValue('Search in last day');
      expect(timeRangeSelect).toBeDisabled();
    });
  });

  it('changes the widget\'s timerange when time range input is used', () => {
    const { getByDisplayValue, getByText } = renderSUT();
    const timeRangeSelect = getByDisplayValue('Search in last day');
    expect(timeRangeSelect).not.toBeNull();

    // $FlowFixMe: We know it is an input, not a plain HTML element
    const optionForAllMessages: HTMLInputElement = getByText('Search in all messages');

    fireEvent.change(timeRangeSelect, { target: { value: optionForAllMessages.value } });

    expect(WidgetActions.timerange).toHaveBeenCalledWith('deadbeef', { type: 'relative', range: '0' });
    expect(getByDisplayValue('Search in all messages')).not.toBeNull();
  });

  it('changes the widget\'s timerange type when switching to absolute time range', () => {
    const { getByText } = renderSUT();
    const absoluteTimeRangeSelect = getByText('Absolute');
    expect(absoluteTimeRangeSelect).not.toBeNull();

    fireEvent.click(absoluteTimeRangeSelect);

    expect(WidgetActions.timerange).toHaveBeenLastCalledWith('deadbeef', { type: 'absolute', from: '2019-10-10T12:21:31.146Z', to: '2019-10-10T12:26:31.146Z' });
  });

  it('changes the widget\'s timerange type when switching to absolute time range', () => {
    const { getByText } = renderSUT();
    const absoluteTimeRangeSelect = getByText('Absolute');
    expect(absoluteTimeRangeSelect).not.toBeNull();

    fireEvent.click(absoluteTimeRangeSelect);

    expect(WidgetActions.timerange).toHaveBeenLastCalledWith('deadbeef', { type: 'absolute', from: '2019-10-10T12:21:31.146Z', to: '2019-10-10T12:26:31.146Z' });
  });

  it('changes the widget\'s streams when using stream filter', async () => {
    const { container } = renderSUT({
      availableStreams: [
        { key: 'PFLog', value: '5c2e27d6ba33a9681ad62775' },
        { key: 'DNS Logs', value: '5d2d9649e117dc4df84cf83c' },
      ],
    });
    const streamFilter = container.querySelector('div[data-testid="streams-filter"] > div');
    expect(streamFilter).not.toBeNull();

    // $FlowFixMe: `streamFilter` cannot be `null` at this point
    await selectEvent.select(streamFilter, 'PFLog');

    expect(WidgetActions.streams).toHaveBeenCalledWith('deadbeef', ['5c2e27d6ba33a9681ad62775']);
  });
});

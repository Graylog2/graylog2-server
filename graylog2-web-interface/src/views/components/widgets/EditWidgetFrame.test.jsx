// @flow strict
import * as React from 'react';
import { render, fireEvent, waitFor } from 'wrappedTestingLibrary';
import selectEvent from 'react-select-event';
import MockStore from 'helpers/mocking/StoreMock';

import { WidgetActions } from 'views/stores/WidgetStore';
import Widget from 'views/logic/widgets/Widget';

import EditWidgetFrame from './EditWidgetFrame';

import ViewTypeContext from '../contexts/ViewTypeContext';
import WidgetContext from '../contexts/WidgetContext';

jest.mock('views/stores/WidgetStore', () => ({
  WidgetActions: {
    update: jest.fn(),
  },
}));

jest.mock('views/stores/SearchConfigStore', () => ({
  SearchConfigStore: MockStore(['getInitialState', () => ({
    searchesClusterConfig: {
      relative_timerange_options: { P1D: 'Search in last day', PT0S: 'Search in all messages' },
      query_time_range_limit: 'PT0S',
    },
  })]),
}));

jest.mock('views/stores/StreamsStore', () => ({
  StreamsStore: MockStore(['getInitialState', () => ({
    streams: [
      { title: 'PFLog', id: '5c2e27d6ba33a9681ad62775' },
      { title: 'DNS Logs', id: '5d2d9649e117dc4df84cf83c' },
    ],
  })]),
}));

jest.mock('moment', () => {
  const mockMoment = jest.requireActual('moment');

  return Object.assign(() => mockMoment('2019-10-10T12:26:31.146Z'), mockMoment);
});

describe('EditWidgetFrame', () => {
  describe('on a dashboard', () => {
    const widget = Widget.builder()
      .id('deadbeef')
      .type('dummy')
      .config({})
      .build();
    const renderSUT = () => render((
      <ViewTypeContext.Provider value="DASHBOARD">
        <WidgetContext.Provider value={widget}>
          <EditWidgetFrame>
            <>Hello World!</>
            <>These are some buttons!</>
          </EditWidgetFrame>
        </WidgetContext.Provider>
      </ViewTypeContext.Provider>
    ));

    // it('changes the widget\'s timerange when time range input is used', async () => {
    //   const { getByDisplayValue, getByText, getByTitle } = renderSUT();
    //   const timeRangeSelect = getByDisplayValue('Search in last day');
    //
    //   expect(timeRangeSelect).not.toBeNull();
    //
    //   const optionForAllMessages = asElement(getByText('Search in all messages'), HTMLOptionElement);
    //
    //   fireEvent.change(timeRangeSelect, { target: { value: optionForAllMessages.value } });
    //
    //   const searchButton = getByTitle(/Perform search/);
    //
    //   fireEvent.click(searchButton);
    //
    //   await waitFor(() => expect(WidgetActions.update).toHaveBeenCalledWith('deadbeef', expect.objectContaining({
    //     timerange: { type: 'relative', range: 0 },
    //   })));
    // });

    // it('changes the widget\'s timerange type when switching to absolute time range', async () => {
    //   const { getByText, getByTitle } = renderSUT();
    //   const absoluteTimeRangeSelect = getByText('Absolute');
    //
    //   expect(absoluteTimeRangeSelect).not.toBeNull();
    //
    //   fireEvent.click(absoluteTimeRangeSelect);
    //
    //   const searchButton = getByTitle(/Perform search/);
    //
    //   fireEvent.click(searchButton);
    //
    //   await waitFor(() => expect(WidgetActions.update)
    //     .toHaveBeenLastCalledWith('deadbeef', expect.objectContaining({
    //       timerange: {
    //         type: 'absolute',
    //         from: '2019-10-10T12:21:31.146Z',
    //         to: '2019-10-10T12:26:31.146Z',
    //       },
    //     })));
    // });

    it('changes the widget\'s streams when using stream filter', async () => {
      const { getByTitle, getByTestId } = renderSUT();
      const streamFilter = getByTestId('streams-filter');
      const reactSelect = streamFilter.querySelector('div');

      expect(reactSelect).not.toBeNull();

      // Flow is not parsing the jest assertion before
      if (reactSelect) {
        await selectEvent.select(reactSelect, 'PFLog');
      }

      const searchButton = getByTitle(/Perform search/);

      fireEvent.click(searchButton);

      await waitFor(() => expect(WidgetActions.update).toHaveBeenCalledWith('deadbeef', expect.objectContaining({
        streams: ['5c2e27d6ba33a9681ad62775'],
      })));
    });
  });
});

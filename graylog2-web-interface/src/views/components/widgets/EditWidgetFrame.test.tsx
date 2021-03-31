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
import selectEvent from 'react-select-event';
import MockStore from 'helpers/mocking/StoreMock';

import { WidgetActions } from 'views/stores/WidgetStore';
import { SearchActions } from 'views/stores/SearchStore';
import Widget from 'views/logic/widgets/Widget';

import EditWidgetFrame from './EditWidgetFrame';

import ViewTypeContext from '../contexts/ViewTypeContext';
import WidgetContext from '../contexts/WidgetContext';

jest.mock('views/stores/WidgetStore', () => ({
  WidgetActions: {
    update: jest.fn(),
  },
}));

jest.mock('views/stores/SearchStore', () => ({
  SearchActions: {
    refresh: jest.fn(),
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
          <EditWidgetFrame onCancel={() => {}} onFinish={() => {}}>
            <>Hello World!</>
            <>These are some buttons!</>
          </EditWidgetFrame>
        </WidgetContext.Provider>
      </ViewTypeContext.Provider>
    ));

    it('performs search when clicking on search button', async () => {
      renderSUT();
      const searchButton = screen.getByTitle(/Perform search/);

      fireEvent.click(searchButton);

      await waitFor(() => expect(SearchActions.refresh).toHaveBeenCalledTimes(1));
    });

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

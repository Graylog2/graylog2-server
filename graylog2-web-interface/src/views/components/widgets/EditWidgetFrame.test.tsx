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
import { createElasticsearchQueryString } from 'views/logic/queries/Query';

import EditWidgetFrame from './EditWidgetFrame';

import ViewTypeContext from '../contexts/ViewTypeContext';
import WidgetContext from '../contexts/WidgetContext';

jest.mock('views/logic/fieldtypes/useFieldTypes');

jest.mock('views/stores/WidgetStore', () => ({
  WidgetActions: {
    update: jest.fn(),
  },
}));

jest.mock('views/stores/SearchStore', () => ({
  SearchStore: MockStore(['getInitialState', () => ({ search: { parameters: [] } })]),
  SearchActions: {
    refresh: jest.fn(() => Promise.resolve()),
  },
}));

jest.mock('views/stores/SearchConfigStore', () => ({
  SearchConfigActions: {
    refresh: jest.fn(() => Promise.resolve()),
  },
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
      .query(createElasticsearchQueryString())
      .timerange({ type: 'relative', from: 300 })
      .config({})
      .build();
    const renderSUT = (props?: Partial<React.ComponentProps<typeof EditWidgetFrame>>) => render((
      <ViewTypeContext.Provider value="DASHBOARD">
        <WidgetContext.Provider value={widget}>
          <EditWidgetFrame onSubmit={() => {}} onCancel={() => {}} {...props}>
            Hello World!
            These are some buttons!
          </EditWidgetFrame>
        </WidgetContext.Provider>
      </ViewTypeContext.Provider>
    ));

    it('refreshes search after clicking on search button, when there are no changes', async () => {
      renderSUT();
      const searchButton = await screen.findByRole('button', { name: /perform search/i });

      await waitFor(() => expect(searchButton).not.toHaveClass('disabled'));
      fireEvent.click(searchButton);

      await waitFor(() => expect(SearchActions.refresh).toHaveBeenCalledTimes(1));
    });

    it('changes the widget\'s streams when using stream filter', async () => {
      renderSUT();
      const streamFilter = await screen.findByTestId('streams-filter');
      const reactSelect = streamFilter.querySelector('div');

      expect(reactSelect).not.toBeNull();

      if (reactSelect) {
        await selectEvent.select(reactSelect, 'PFLog');
      }

      const searchButton = screen.getByRole('button', {
        name: /perform search \(changes were made after last search execution\)/i,
      });
      await waitFor(() => expect(searchButton).not.toHaveClass('disabled'));

      fireEvent.click(searchButton);

      await waitFor(() => expect(WidgetActions.update).toHaveBeenCalledWith('deadbeef', expect.objectContaining({
        streams: ['5c2e27d6ba33a9681ad62775'],
      })));
    });

    it('calls onSubmit', async () => {
      const onSubmit = jest.fn();
      renderSUT({ onSubmit });

      fireEvent.click(await screen.findByRole('button', { name: /update widget/i }));

      await waitFor(() => expect(onSubmit).toHaveBeenCalledTimes(1));
    });

    it('calls onCancel', async () => {
      const onCancel = jest.fn();
      renderSUT({ onCancel });

      fireEvent.click(await screen.findByRole('button', { name: /cancel/i }));

      await waitFor(() => expect(onCancel).toHaveBeenCalledTimes(1));
    });

    it('does not display submit and cancel button when `displaySubmitActions` is false', async () => {
      renderSUT({ displaySubmitActions: false });

      expect(screen.queryByRole('button', { name: /update widget/i })).not.toBeInTheDocument();
      expect(screen.queryByRole('button', { name: /cancel/i })).not.toBeInTheDocument();
    });
  });
});

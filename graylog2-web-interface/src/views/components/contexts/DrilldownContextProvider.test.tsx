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
import { useContext } from 'react';
import { mount } from 'wrappedEnzyme';
import { StoreMock as mockStore, asMock } from 'helpers/mocking';

import View from 'views/logic/views/View';
import MessagesWidget from 'views/logic/widgets/MessagesWidget';
import Query, { createElasticsearchQueryString, filtersForQuery } from 'views/logic/queries/Query';
import { GlobalOverrideStore } from 'views/stores/GlobalOverrideStore';
import GlobalOverride from 'views/logic/search/GlobalOverride';
import { CurrentQueryStore } from 'views/stores/CurrentQueryStore';

import DrilldownContextProvider from './DrilldownContextProvider';
import DrilldownContext from './DrilldownContext';
import ViewTypeContext from './ViewTypeContext';

jest.mock('views/stores/CurrentQueryStore', () => ({
  CurrentQueryStore: mockStore(['listen', () => () => {}], ['getInitialState', jest.fn(() => null)]),
}));

jest.mock('views/stores/GlobalOverrideStore', () => ({
  GlobalOverrideStore: mockStore(['listen', () => () => {}], ['getInitialState', jest.fn()]),
}));

describe('DrilldownContextProvider', () => {
  // eslint-disable-next-line no-unused-vars
  const Consumer = ({ streams, timerange, query }) => null;

  const TestComponent = () => {
    const { streams, timerange, query } = useContext(DrilldownContext);

    return <Consumer streams={streams} timerange={timerange} query={query} />;
  };

  const widget = MessagesWidget.builder()
    .streams(['stream1', 'stream2'])
    .timerange({ type: 'relative', range: 1800 })
    .query(createElasticsearchQueryString('foo:42'))
    .build();

  const expectDrilldown = (expectedStreams, expectedTimerange, expectedQuery, wrapper) => {
    const consumer = wrapper.find('Consumer');
    const { streams, timerange, query } = consumer.props();

    expect(streams).toEqual(expectedStreams);
    expect(timerange).toEqual(expectedTimerange);
    expect(query).toEqual(expectedQuery);
  };

  const renderSUT = (viewType) => mount(
    <ViewTypeContext.Provider value={viewType}>
      <DrilldownContextProvider widget={widget}>
        <TestComponent />
      </DrilldownContextProvider>
    </ViewTypeContext.Provider>,
  );

  describe('if current view is a dashboard', () => {
    it('passes current query, streams & timerange of widget if global override is not set', () => {
      const wrapper = renderSUT(View.Type.Dashboard);

      expectDrilldown(['stream1', 'stream2'],
        { type: 'relative', range: 1800 },
        { type: 'elasticsearch', query_string: 'foo:42' },
        wrapper);
    });

    it('passes query & timerange of global override, streams of widget', () => {
      asMock(GlobalOverrideStore.getInitialState)
        .mockReturnValue(GlobalOverride.create(
          { type: 'absolute', from: '2020-01-10T13:23:42.000Z', to: '2020-01-10T14:23:42.000Z' },
          createElasticsearchQueryString('something:"else"'),
        ));

      const wrapper = renderSUT(View.Type.Dashboard);

      expectDrilldown(['stream1', 'stream2'],
        { type: 'absolute', from: '2020-01-10T13:23:42.000Z', to: '2020-01-10T14:23:42.000Z' },
        { type: 'elasticsearch', query_string: 'something:"else"' },
        wrapper);
    });
  });

  describe('if current view is a search', () => {
    it('passes default values if no current query is present', () => {
      const wrapper = renderSUT(View.Type.Search);

      expectDrilldown([],
        { type: 'relative', range: 300 },
        { type: 'elasticsearch', query_string: '' },
        wrapper);
    });

    it('passes values from current query if present', () => {
      const query = Query.builder()
        .query(createElasticsearchQueryString('foo:"bar"'))
        // $FlowFixMe: We know it is defined
        .filter(filtersForQuery(['onestream', 'anotherstream']))
        .timerange({ type: 'keyword', keyword: 'last year' })
        .build();

      asMock(CurrentQueryStore.getInitialState).mockReturnValueOnce(query);
      const wrapper = renderSUT(View.Type.Search);

      expectDrilldown(['onestream', 'anotherstream'],
        { type: 'keyword', keyword: 'last year' },
        { type: 'elasticsearch', query_string: 'foo:"bar"' },
        wrapper);
    });
  });
});

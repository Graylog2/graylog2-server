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
import { render, waitFor } from 'wrappedTestingLibrary';

import { asMock } from 'helpers/mocking';
import View from 'views/logic/views/View';
import MessagesWidget from 'views/logic/widgets/MessagesWidget';
import Query, { createElasticsearchQueryString, filtersForQuery } from 'views/logic/queries/Query';
import GlobalOverride from 'views/logic/search/GlobalOverride';
import useViewType from 'views/hooks/useViewType';
import useCurrentQuery from 'views/logic/queries/useCurrentQuery';
import useGlobalOverride from 'views/hooks/useGlobalOverride';

import DrilldownContextProvider from './DrilldownContextProvider';
import type { Drilldown } from './DrilldownContext';
import DrilldownContext from './DrilldownContext';

jest.mock('views/hooks/useViewType');
jest.mock('views/logic/queries/useCurrentQuery');
jest.mock('views/hooks/useGlobalOverride');

describe('DrilldownContextProvider', () => {
  const widget = MessagesWidget.builder()
    .streams(['stream1', 'stream2'])
    .timerange({ type: 'relative', range: 1800 })
    .query(createElasticsearchQueryString('foo:42'))
    .build();

  const renderSUT = async () => {
    const consumer = jest.fn();
    render(
      <DrilldownContextProvider widget={widget}>
        <DrilldownContext.Consumer>{consumer}</DrilldownContext.Consumer>
      </DrilldownContextProvider>,
    );
    await waitFor(() => {
      expect(consumer).toHaveBeenCalled();
    });

    return asMock(consumer).mock.calls[0][0] as Drilldown;
  };

  describe('if current view is a dashboard', () => {
    it('passes current query, streams & timerange of widget if global override is not set', async () => {
      asMock(useViewType).mockReturnValue(View.Type.Dashboard);

      const observedValues = await renderSUT();

      expect(observedValues).toEqual({
        streams: ['stream1', 'stream2'],
        timerange: { type: 'relative', range: 1800 },
        query: { type: 'elasticsearch', query_string: 'foo:42' },
      });
    });

    it('passes timerange of global override, streams of widget and combined query of global override and widget', async () => {
      asMock(useViewType).mockReturnValue(View.Type.Dashboard);

      asMock(useGlobalOverride).mockReturnValue(
        GlobalOverride.create(
          { type: 'absolute', from: '2020-01-10T13:23:42.000Z', to: '2020-01-10T14:23:42.000Z' },
          createElasticsearchQueryString('something:"else"'),
        ),
      );

      const observedValues = await renderSUT();

      expect(observedValues).toEqual({
        streams: ['stream1', 'stream2'],
        timerange: { type: 'absolute', from: '2020-01-10T13:23:42.000Z', to: '2020-01-10T14:23:42.000Z' },
        query: { type: 'elasticsearch', query_string: '(foo:42) AND (something:"else")' },
      });
    });
  });

  describe('if current view is a search', () => {
    it('passes default values if no current query is present', async () => {
      asMock(useViewType).mockReturnValue(View.Type.Search);
      asMock(useCurrentQuery).mockReturnValue(undefined);

      const observedValues = await renderSUT();

      expect(observedValues).toEqual(
        expect.objectContaining({
          streams: [],
          timerange: { type: 'relative', from: 300 },
          query: { type: 'elasticsearch', query_string: '' },
        }),
      );
    });

    it('passes values from current query if present', async () => {
      asMock(useViewType).mockReturnValue(View.Type.Search);

      const query = Query.builder()
        .query(createElasticsearchQueryString('foo:"bar"'))
        .filter(filtersForQuery(['onestream', 'anotherstream']))
        .timerange({ type: 'keyword', keyword: 'last year' })
        .build();

      asMock(useCurrentQuery).mockReturnValue(query);

      const observedValues = await renderSUT();

      expect(observedValues).toEqual(
        expect.objectContaining({
          streams: ['onestream', 'anotherstream'],
          timerange: { type: 'keyword', keyword: 'last year' },
          query: { type: 'elasticsearch', query_string: 'foo:"bar"' },
        }),
      );
    });
  });
});

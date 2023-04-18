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
import { useMemo } from 'react';
import * as Immutable from 'immutable';

import View from 'views/logic/views/View';
import type { AbsoluteTimeRange, ElasticsearchQueryString, RelativeTimeRangeStartOnly } from 'views/logic/queries/Query';
import type { Event } from 'components/events/events/types';
import type { EventDefinition } from 'logic/alerts/types';
import QueryGenerator from 'views/logic/queries/QueryGenerator';
import Search from 'views/logic/search/Search';
import { matchesDecoratorStream } from 'views/logic/views/ViewStateGenerator';
import UpdateSearchForWidgets from 'views/logic/views/UpdateSearchForWidgets';
import ViewState from 'views/logic/views/ViewState';
import { allMessagesTable, resultHistogram } from 'views/logic/Widgets';
import WidgetPosition from 'views/logic/widgets/WidgetPosition';
import { DecoratorsActions } from 'stores/decorators/DecoratorsStore';
import type { ParameterJson } from 'views/logic/parameters/Parameter';
import Parameter from 'views/logic/parameters/Parameter';
import { concatQueryStrings, escape } from 'views/logic/queries/QueryHelper';

export const WidgetsGenerator = async ({ streams }) => {
  const decorators = await DecoratorsActions.list();
  const byStreamId = matchesDecoratorStream(streams);
  const streamDecorators = decorators?.length ? decorators.filter(byStreamId) : [];
  const histogram = resultHistogram();
  const messageTable = allMessagesTable(undefined, streamDecorators);

  const widgets = [
    histogram,
    messageTable,
  ];

  const titles = {
    widget: {
      [histogram.id]: 'Message Count',
      [messageTable.id]: 'All Messages',
    },
  };

  const positions = {
    [histogram.id]: new WidgetPosition(1, 1, 2, Infinity),
    [messageTable.id]: new WidgetPosition(1, 3, 6, Infinity),
  };

  return { titles, widgets, positions };
};

export const ViewStateGenerator = async ({ streams }: { streams: string | string[] | undefined }) => {
  const { titles, widgets, positions } = await WidgetsGenerator({ streams });

  return ViewState.create()
    .toBuilder()
    .titles(titles)
    .widgets(Immutable.List(widgets))
    .widgetPositions(positions)
    .build();
};

export const ViewGenerator = async ({
  streams,
  timeRange,
  queryString,
  queryParameters,
}: {
  streams: string | string[] | undefined | null,
  timeRange: AbsoluteTimeRange | RelativeTimeRangeStartOnly,
  queryString: ElasticsearchQueryString,
  queryParameters: Array<ParameterJson>,
},
) => {
  const query = QueryGenerator(streams, undefined, timeRange, queryString);
  const search = Search.create().toBuilder().queries([query]).parameters(queryParameters.map((param) => Parameter.fromJSON(param)))
    .build();
  const viewState = await ViewStateGenerator({ streams });

  const view = View.create()
    .toBuilder()
    .newId()
    .type(View.Type.Search)
    .state({ [query.id]: viewState })
    .search(search)
    .build();

  return UpdateSearchForWidgets(view);
};

export const UseCreateViewForEvent = (
  { eventData, eventDefinition }: { eventData: Event, eventDefinition: EventDefinition },
) => {
  const queryStringFromGrouping = concatQueryStrings(Object.entries(eventData.group_by_fields).map(([field, value]) => `${field}:${escape(value)}`), { withBrackets: false });
  const eventQueryString = eventData?.replay_info?.query || '';
  const { streams } = eventData.replay_info;
  const timeRange: AbsoluteTimeRange = {
    type: 'absolute',
    from: eventData?.replay_info?.timerange_start,
    to: eventData?.replay_info?.timerange_end,
  };
  const queryString: ElasticsearchQueryString = {
    type: 'elasticsearch',
    query_string: concatQueryStrings([eventQueryString, queryStringFromGrouping]),
  };

  const queryParameters = eventDefinition?.config?.query_parameters || [];

  return useMemo(
    () => ViewGenerator({ streams, timeRange, queryString, queryParameters }),
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [],
  );
};

export default UseCreateViewForEvent;

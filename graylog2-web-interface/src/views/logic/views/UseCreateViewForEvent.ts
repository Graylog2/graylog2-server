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
import uniq from 'lodash/uniq';

import View from 'views/logic/views/View';
import type { AbsoluteTimeRange, ElasticsearchQueryString, RelativeTimeRangeStartOnly } from 'views/logic/queries/Query';
import type { EventType } from 'hooks/useEventById';
import type { EventDefinition } from 'logic/alerts/types';
import QueryGenerator from 'views/logic/queries/QueryGenerator';
import Search from 'views/logic/search/Search';
import { matchesDecoratorStream } from 'views/logic/views/ViewStateGenerator';
import UpdateSearchForWidgets from 'views/logic/views/UpdateSearchForWidgets';
import ViewState from 'views/logic/views/ViewState';
import { allMessagesTable, resultHistogram } from 'views/logic/Widgets';
import WidgetPosition from 'views/logic/widgets/WidgetPosition';
import { DecoratorsActions } from 'stores/decorators/DecoratorsStore';
import generateId from 'logic/generateId';
import pivotForField from 'views/logic/searchtypes/aggregation/PivotGenerator';
import FieldType from 'views/logic/fieldtypes/FieldType';
import AggregationWidget from 'views/logic/aggregationbuilder/AggregationWidget';
import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import Series from 'views/logic/aggregationbuilder/Series';
import type Pivot from 'views/logic/aggregationbuilder/Pivot';
import type { EventDefinitionAggregation } from 'hooks/useEventDefinition';
import SortConfig from 'views/logic/aggregationbuilder/SortConfig';
import Direction from 'views/logic/aggregationbuilder/Direction';

const AGGREGATION_WIDGET_HEIGHT = 3;

const getAggregationWidget = ({ rowPivots, fnSeries, sort = [] }: {
  rowPivots: Array<Pivot>,
  fnSeries: Array<Series>,
  sort?: Array<SortConfig>
}) => AggregationWidget.builder()
  .id(generateId())
  .config(
    AggregationWidgetConfig.builder()
      .columnPivots([])
      .rowPivots(rowPivots)
      .series(fnSeries)
      .sort(sort)
      .visualization('table')
      .rollup(true)
      .build(),
  )
  .build();

const WidgetsGenerator = async ({ streams, aggregations, groupBy }) => {
  const decorators = await DecoratorsActions.list();
  const byStreamId = matchesDecoratorStream(streams);
  const streamDecorators = decorators ? decorators.filter(byStreamId) : [];
  const histogram = resultHistogram();
  const messageTable = allMessagesTable(undefined, streamDecorators);
  const summaryAggregations = {
    fnSeries: [],
    rowPivots: [],
    title: 'Summary: ',
  };
  const needsSummaryAggregations = aggregations.length > 1;
  const SUMMARY_ROW_DELTA = needsSummaryAggregations ? AGGREGATION_WIDGET_HEIGHT : 0;
  const { aggregationWidgets, aggregationTitles, aggregationPositions } = aggregations.reduce((res, { field, value, expr, fnSeries }, index) => {
    const rowPivots = [pivotForField(uniq([field, ...groupBy]), new FieldType('value', [], []))];
    const fnSeriesForFunc = Series.forFunction(fnSeries);
    const direction = ['>', '>=', '=='].includes(expr) ? Direction.Descending : Direction.Ascending;
    const sort = [new SortConfig(SortConfig.SERIES_TYPE, fnSeries, direction)];
    const widget = getAggregationWidget({ rowPivots, fnSeries: [fnSeriesForFunc], sort });
    const widgetId = widget.id;
    const title = `${fnSeries} ${expr} ${value}`;
    const isEven = (index + 1) % 2 === 0;
    const col = isEven ? 7 : 1;
    const row = Math.ceil((index + 1) / 2) + AGGREGATION_WIDGET_HEIGHT + SUMMARY_ROW_DELTA;
    const position = new WidgetPosition(col, row, AGGREGATION_WIDGET_HEIGHT, 6);
    res.aggregationWidgets.push(widget);
    res.aggregationTitles[widgetId] = title;
    res.aggregationPositions[widgetId] = position;

    if (needsSummaryAggregations) {
      summaryAggregations.fnSeries.push(fnSeries);
      summaryAggregations.rowPivots.push(field);
      summaryAggregations.title = `${summaryAggregations.title} ${title}`;
    }

    return res;
  }, { aggregationTitles: {}, aggregationWidgets: [], aggregationPositions: {} });

  const widgets = [
    ...aggregationWidgets,
    histogram,
    messageTable,
  ];

  const titles = {
    widget: {
      ...aggregationTitles,
      [histogram.id]: 'Message Count',
      [messageTable.id]: 'All Messages',
    },
  };

  const positions = {
    ...aggregationPositions,
    [histogram.id]: new WidgetPosition(1, AGGREGATION_WIDGET_HEIGHT * aggregationWidgets.length + 1 + SUMMARY_ROW_DELTA, 2, Infinity),
    [messageTable.id]: new WidgetPosition(1, AGGREGATION_WIDGET_HEIGHT * aggregationWidgets.length + 3 + SUMMARY_ROW_DELTA, 6, Infinity),
  };

  if (needsSummaryAggregations) {
    const summaryAggregationWidget = getAggregationWidget({
      rowPivots: [pivotForField(uniq([...summaryAggregations.rowPivots, ...groupBy]), new FieldType('value', [], []))],
      fnSeries: summaryAggregations.fnSeries.map((s) => Series.forFunction(s)),
    });
    widgets.push(summaryAggregationWidget);
    titles.widget[summaryAggregationWidget.id] = summaryAggregations.title;
    positions[summaryAggregationWidget.id] = new WidgetPosition(1, 1, AGGREGATION_WIDGET_HEIGHT, Infinity);
  }

  return { titles, widgets, positions };
};

const ViewStateGenerator = async ({ streams, aggregations, groupBy }: {groupBy: Array<string>, streams: string | string[] | undefined, aggregations: Array<any>}) => {
  const { titles, widgets, positions } = await WidgetsGenerator({ streams, aggregations, groupBy });

  return ViewState.create()
    .toBuilder()
    .titles(titles)
    .widgets(Immutable.List(widgets))
    .widgetPositions(positions)
    .build();
};

const ViewGenerator = async ({
  streams,
  timeRange,
  queryString,
  aggregations,
  groupBy,
}: {
  streams: string | string[] | undefined | null,
  timeRange: AbsoluteTimeRange | RelativeTimeRangeStartOnly,
  queryString: ElasticsearchQueryString,
  aggregations: Array<EventDefinitionAggregation>
  groupBy: Array<string>
},
) => {
  const query = QueryGenerator(streams, undefined, timeRange, queryString);
  const search = Search.create().toBuilder().queries([query]).build();
  const viewState = await ViewStateGenerator({ streams, aggregations, groupBy });

  const view = View.create()
    .toBuilder()
    .newId()
    .type(View.Type.Search)
    .state({ [query.id]: viewState })
    .search(search)
    .build();

  return UpdateSearchForWidgets(view);
};

const useCreateViewForEvent = (
  { eventData, eventDefinition, aggregations }: { eventData: EventType, eventDefinition: EventDefinition, aggregations: Array<EventDefinitionAggregation> },
) => {
  const { streams } = eventData.replay_info;
  const timeRange: AbsoluteTimeRange = {
    type: 'absolute',
    from: eventData?.replay_info?.timerange_start,
    to: eventData?.replay_info?.timerange_end,
  };
  const queryString: ElasticsearchQueryString = {
    type: 'elasticsearch',
    query_string: eventData?.replay_info?.query || '',
  };
  const groupBy = eventDefinition.config.group_by;

  return useMemo(
    () => ViewGenerator({ streams, timeRange, queryString, aggregations, groupBy }),
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [],
  );
};

export const useCreateViewForEventDefinition = (
  { eventDefinition, aggregations }: { eventDefinition: EventDefinition, aggregations: Array<EventDefinitionAggregation> },
) => {
  const { streams } = eventDefinition.config;
  const timeRange: RelativeTimeRangeStartOnly = {
    type: 'relative',
    range: eventDefinition.config.search_within_ms / 1000,
  };
  const queryString: ElasticsearchQueryString = {
    type: 'elasticsearch',
    query_string: eventDefinition.config.query || '',
  };
  const groupBy = eventDefinition.config.group_by;

  return useMemo(
    () => ViewGenerator({ streams, timeRange, queryString, aggregations, groupBy }),
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [],
  );
};

export default useCreateViewForEvent;

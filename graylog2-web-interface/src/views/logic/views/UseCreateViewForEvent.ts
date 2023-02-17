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
import type { ElasticsearchQueryString, TimeRange } from 'views/logic/queries/Query';
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

const NEW_WIDGET_HEIGHT = 2;

const transformExpressionsToArray = ({ series, conditions }) => {
  const res = [];

  const rec = (expression) => {
    if (!expression) {
      return 'No condition configured';
    }

    switch (expression.expr) {
      case 'number':
        return ({ value: expression.value });
      case 'number-ref':
        // eslint-disable-next-line no-case-declarations
        const selectedSeriesk = series.find((s) => s.id === expression.ref);

        return (selectedSeriesk && selectedSeriesk.function
          ? { field: `${selectedSeriesk.function}(${selectedSeriesk.field})` }
          : null);
      case '&&':
      case '||':
        return [rec(expression.left), rec(expression.right)];
      case 'group':
        return [rec(expression.child)];
      case '<':
      case '<=':
      case '>':
      case '>=':
      case '==':
        // eslint-disable-next-line no-case-declarations
        const { ref } = expression.left;
        // eslint-disable-next-line no-case-declarations
        const selectedSeries = series.find((s) => s.id === ref);
        // eslint-disable-next-line no-case-declarations
        const fnSeries = selectedSeries && selectedSeries?.function ? `${selectedSeries.function}(${selectedSeries.field})` : undefined;
        res.push({ expr: expression.expr, value: expression.right.value, function: selectedSeries?.function, fnSeries, field: selectedSeries.field });

        return [rec(expression.left), rec(expression.right)];
      default:
        return null;
    }
  };

  rec(conditions.expression);

  return res;
};

const getAggregationWidget = ({ rowPivots, fnSeries }) => AggregationWidget.builder()
  .id(generateId())
  .config(
    AggregationWidgetConfig.builder()
      .columnPivots([])
      .rowPivots(rowPivots)
      .series([
        Series.forFunction(fnSeries),
      ])
      .sort([])
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
  const { aggregationWidgets, aggregationTitles, aggregationositions } = aggregations.reduce((res, { field, value, expr, fnSeries }, index) => {
    const rowPivots = uniq([field, ...groupBy]).map((f) => pivotForField(f, new FieldType('value', [], [])));
    const widget = getAggregationWidget({ rowPivots, fnSeries });
    const widgetId = widget.id;
    const title = `${fnSeries} ${expr} ${value}`;
    const position = new WidgetPosition(1, index + NEW_WIDGET_HEIGHT + 1, NEW_WIDGET_HEIGHT, Infinity);
    res.aggregationWidgets.push(widget);
    res.aggregationTitles[widgetId] = title;
    res.aggregationositions[widgetId] = position;

    return res;
  }, { aggregationTitles: {}, aggregationWidgets: [], aggregationositions: {} });
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
    ...aggregationositions,
    [histogram.id]: new WidgetPosition(1, NEW_WIDGET_HEIGHT * aggregationWidgets.length + 1, 2, Infinity),
    [messageTable.id]: new WidgetPosition(1, NEW_WIDGET_HEIGHT * aggregationWidgets.length + 3, 6, Infinity),
  };

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
  timeRange: TimeRange,
  queryString: ElasticsearchQueryString,
  aggregations: Array<string>
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
  { eventData, EDData }: { eventData: EventType, EDData: EventDefinition },
) => {
  const { streams } = eventData.replay_info;
  const timeRange = {
    type: 'absolute',
    from: eventData?.replay_info?.timerange_start,
    to: eventData?.replay_info?.timerange_end,
  };
  const queryString = {
    type: 'elasticsearch',
    query_string: eventData?.replay_info?.query || ' ',
  };
  const aggregations = transformExpressionsToArray({ series: EDData.config.series, conditions: EDData.config.conditions });
  const groupBy = EDData.config.group_by;

  return useMemo(
    () => ViewGenerator({ streams, timeRange, queryString, aggregations, groupBy }),
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [],
  );
};

export default useCreateViewForEvent;

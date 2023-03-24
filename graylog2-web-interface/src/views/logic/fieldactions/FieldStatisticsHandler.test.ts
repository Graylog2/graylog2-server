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
import Widget from 'views/logic/widgets/Widget';
import TitleTypes from 'views/stores/TitleTypes';
import { createViewWithWidgets } from 'fixtures/searches';
import AggregationWidget from 'views/logic/aggregationbuilder/AggregationWidget';
import mockDispatch from 'views/test/mockDispatch';
import type { RootState } from 'views/types';
import { addWidget } from 'views/logic/slices/widgetActions';
import { setTitle } from 'views/logic/slices/titlesActions';

import handler from './FieldStatisticsHandler';

import FieldType from '../fieldtypes/FieldType';
import { createElasticsearchQueryString } from '../queries/Query';

const numericFieldType = new FieldType('foo', ['numeric'], []);
const nonNumericFieldType = new FieldType('foo', [], []);

const queryId = 'queryId';
const fieldName = 'foo';

jest.mock('views/logic/slices/widgetActions', () => ({
  addWidget: jest.fn(async () => {}),
}));

jest.mock('views/logic/slices/titlesActions', () => ({
  setTitle: jest.fn(async () => {}),
}));

const expectSeries = (func: string) => expect.objectContaining({ function: func });

const expectWidgetWithSeries = (series: Array<string>) => expect.objectContaining({
  config: expect.objectContaining({
    series: series.map(expectSeries),
  }),
});

describe('FieldStatisticsHandler', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  const widget = AggregationWidget.builder().build();
  const view = createViewWithWidgets([widget], {});
  const dispatch = mockDispatch({ view: { view, activeQuery: 'query-id-1' } } as RootState);

  it('creates field statistics widget for given numeric field', async () => {
    await dispatch(handler({ queryId, field: fieldName, type: numericFieldType, contexts: {} }));

    expect(addWidget).toHaveBeenCalledWith(expectWidgetWithSeries([
      `count(${fieldName})`,
      `sum(${fieldName})`,
      `avg(${fieldName})`,
      `min(${fieldName})`,
      `max(${fieldName})`,
      `stddev(${fieldName})`,
      `variance(${fieldName})`,
      `card(${fieldName})`,
      `percentile(${fieldName},95)`,
    ]));
  });

  it('creates field statistics widget for given non-numeric field', async () => {
    await dispatch(handler({ queryId, field: fieldName, type: nonNumericFieldType, contexts: {} }));

    expect(addWidget).toHaveBeenCalledWith(expectWidgetWithSeries([
      `count(${fieldName})`,
      `card(${fieldName})`,
    ]));
  });

  it('creates field statistics widget and copies the widget filter of original widget', async () => {
    const filter = "author: 'Vanth'";
    const origWidget = Widget.builder().filter(filter).build();

    await dispatch(handler({ queryId, field: fieldName, type: nonNumericFieldType, contexts: { widget: origWidget } }));

    expect(addWidget).toHaveBeenCalledWith(expect.objectContaining({ filter }));
  });

  it('adds title to generated widget', async () => {
    await dispatch(handler({ queryId, field: fieldName, type: nonNumericFieldType, contexts: {} }));

    expect(setTitle).toHaveBeenCalledWith('query-id-1', TitleTypes.Widget, expect.any(String), `Field Statistics for ${fieldName}`);
  });

  it('duplicates query/timerange/streams/filter of original widget', async () => {
    const origWidget = Widget.builder()
      .filter('author: "Vanth"')
      .query(createElasticsearchQueryString('foo:42'))
      .streams(['stream1', 'stream23'])
      .timerange({ type: 'relative', range: 3600 })
      .build();

    await dispatch(handler({
      queryId,
      field: fieldName,
      type: nonNumericFieldType,
      contexts: { widget: origWidget },
    }));

    expect(addWidget).toHaveBeenCalledWith(expect.objectContaining({
      filter: 'author: "Vanth"',
      query: expect.objectContaining({ query_string: 'foo:42' }),
      streams: ['stream1', 'stream23'],
      timerange: { type: 'relative', range: 3600 },
    }));
  });
});

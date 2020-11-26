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
import asMock from 'helpers/mocking/AsMock';
import mockAction from 'helpers/mocking/MockAction';

import { WidgetActions } from 'views/stores/WidgetStore';

import AggregateActionHandler from './AggregateActionHandler';

import FieldType from '../fieldtypes/FieldType';
import AggregationWidget from '../aggregationbuilder/AggregationWidget';
import Pivot from '../aggregationbuilder/Pivot';
import Widget from '../widgets/Widget';
import { createElasticsearchQueryString } from '../queries/Query';

jest.mock('views/stores/WidgetStore', () => ({ WidgetActions: {} }));
jest.mock('views/components/datatable/DataTable', () => ({ type: 'table' }));

describe('AggregateActionHandler', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('uses field type when generating widget', () => {
    WidgetActions.create = mockAction(jest.fn((widget: Widget) => Promise.resolve(widget)));
    AggregateActionHandler({ queryId: 'queryId', field: 'foo', type: new FieldType('keyword', [], []), contexts: {} });

    expect(WidgetActions.create).toHaveBeenCalled();

    const widget: AggregationWidget = asMock(WidgetActions.create).mock.calls[0][0];
    const { config } = widget;

    expect(config.rowPivots[0]).toEqual(new Pivot('foo', 'values', { limit: 15 }));
  });

  it('uses field type when generating widget', () => {
    WidgetActions.create = mockAction(jest.fn((widget: Widget) => Promise.resolve(widget)));
    const filter = 'author: "Vanth"';
    const origWidget = Widget.builder().filter(filter).build();

    AggregateActionHandler({ queryId: 'queryId', field: 'foo', type: new FieldType('keyword', [], []), contexts: { widget: origWidget } });

    expect(WidgetActions.create).toHaveBeenCalled();

    const widget: AggregationWidget = asMock(WidgetActions.create).mock.calls[0][0];

    expect(widget.filter).toEqual(filter);
  });

  it('duplicates query/timerange/streams/filter of original widget', () => {
    WidgetActions.create = mockAction(jest.fn((widget: Widget) => Promise.resolve(widget)));
    const origWidget = Widget.builder()
      .filter('author: "Vanth"')
      .query(createElasticsearchQueryString('foo:42'))
      .streams(['stream1', 'stream23'])
      .timerange({ type: 'relative', range: 3600 })
      .build();

    AggregateActionHandler({
      queryId: 'queryId',
      field: 'foo',
      type: new FieldType('keyword', [], []),
      contexts: { widget: origWidget },
    });

    expect(WidgetActions.create).toHaveBeenCalled();

    const { filter, query, streams, timerange }: AggregationWidget = asMock(WidgetActions.create).mock.calls[0][0];

    expect(filter).toEqual('author: "Vanth"');
    expect(query).toEqual(createElasticsearchQueryString('foo:42'));
    expect(streams).toEqual(['stream1', 'stream23']);
    expect(timerange).toEqual({ type: 'relative', range: 3600 });
  });
});

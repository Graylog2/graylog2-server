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
// @flow strict
import asMock from 'helpers/mocking/AsMock';

import { WidgetActions } from 'views/stores/WidgetStore';
import Widget from 'views/logic/widgets/Widget';
import { TitlesActions, TitleTypes } from 'views/stores/TitlesStore';

import handler from './FieldStatisticsHandler';

import FieldType from '../fieldtypes/FieldType';
import { createElasticsearchQueryString } from '../queries/Query';
import AggregationWidget from '../aggregationbuilder/AggregationWidget';

jest.mock('views/stores/WidgetStore', () => ({
  WidgetActions: {
    create: jest.fn((widget) => Promise.resolve(widget)),
  },
}));

jest.mock('views/stores/TitlesStore', () => ({
  TitlesActions: {
    set: jest.fn(() => Promise.resolve()),
  },
  TitleTypes: {
    Widget: 'widget',
  },
}));

const numericFieldType = new FieldType('foo', ['numeric'], []);
const nonNumericFieldType = new FieldType('foo', [], []);

const queryId = 'queryId';
const fieldName = 'foo';

describe('FieldStatisticsHandler', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('creates field statistics widget for given numeric field', () => {
    return handler({ queryId, field: fieldName, type: numericFieldType, contexts: {} }).then(() => {
      expect(WidgetActions.create).toHaveBeenCalled();

      const widget = asMock(WidgetActions.create).mock.calls[0][0];

      expect(widget.config.series.map((s) => s.function)).toEqual([
        `count(${fieldName})`,
        `sum(${fieldName})`,
        `avg(${fieldName})`,
        `min(${fieldName})`,
        `max(${fieldName})`,
        `stddev(${fieldName})`,
        `variance(${fieldName})`,
        `card(${fieldName})`,
        `percentile(${fieldName},95)`,
      ]);
    });
  });

  it('creates field statistics widget for given non-numeric field', () => {
    return handler({ queryId, field: fieldName, type: nonNumericFieldType, contexts: {} }).then(() => {
      expect(WidgetActions.create).toHaveBeenCalled();

      const widget = asMock(WidgetActions.create).mock.calls[0][0];

      expect(widget.config.series.map((s) => s.function)).toEqual([
        `count(${fieldName})`,
        `card(${fieldName})`,
      ]);
    });
  });

  it('creates field statistics widget and copies the widget filter of original widget', () => {
    const filter = "author: 'Vanth'";
    const origWidget = Widget.builder().filter(filter).build();

    return handler({ queryId, field: fieldName, type: nonNumericFieldType, contexts: { widget: origWidget } }).then(() => {
      expect(WidgetActions.create).toHaveBeenCalled();

      const widget = asMock(WidgetActions.create).mock.calls[0][0];

      expect(widget.filter).toEqual(filter);
    });
  });

  it('adds title to generated widget', () => {
    return handler({ queryId, field: fieldName, type: nonNumericFieldType, contexts: {} }).then(() => {
      const widget = asMock(WidgetActions.create).mock.calls[0][0];

      expect(TitlesActions.set).toHaveBeenCalledWith(TitleTypes.Widget, widget.id, `Field Statistics for ${fieldName}`);
    });
  });

  it('duplicates query/timerange/streams/filter of original widget', () => {
    const origWidget = Widget.builder()
      .filter('author: "Vanth"')
      .query(createElasticsearchQueryString('foo:42'))
      .streams(['stream1', 'stream23'])
      .timerange({ type: 'relative', range: 3600 })
      .build();

    return handler({
      queryId,
      field: fieldName,
      type: nonNumericFieldType,
      contexts: { widget: origWidget },
    }).then(() => {
      expect(WidgetActions.create).toHaveBeenCalled();

      const { filter, query, streams, timerange }: AggregationWidget = asMock(WidgetActions.create).mock.calls[0][0];

      expect(filter).toEqual('author: "Vanth"');
      expect(query).toEqual(createElasticsearchQueryString('foo:42'));
      expect(streams).toEqual(['stream1', 'stream23']);
      expect(timerange).toEqual({ type: 'relative', range: 3600 });
    });
  });
});

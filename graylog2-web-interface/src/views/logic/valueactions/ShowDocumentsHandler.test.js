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

import FieldType from 'views/logic/fieldtypes/FieldType';
import { TitlesActions } from 'views/stores/TitlesStore';
import TitleTypes from 'views/stores/TitleTypes';
import { WidgetActions } from 'views/stores/WidgetStore';

import ShowDocumentsHandler from './ShowDocumentsHandler';

import AggregationWidget from '../aggregationbuilder/AggregationWidget';
import AggregationWidgetConfig from '../aggregationbuilder/AggregationWidgetConfig';
import PivotGenerator from '../searchtypes/aggregation/PivotGenerator';
import { createElasticsearchQueryString } from '../queries/Query';
import Widget from '../widgets/Widget';

jest.mock('views/stores/WidgetStore', () => ({
  WidgetActions: {
    create: jest.fn((widget) => Promise.resolve(widget)),
  },
}));

jest.mock('views/stores/TitlesStore', () => ({
  TitlesActions: {
    set: jest.fn(() => Promise.resolve()),
  },
}));

const queryId = 'someQuery';
const field = 'foo';

const widgetConfig = AggregationWidgetConfig
  .builder()
  .rowPivots([
    PivotGenerator('bar', FieldType.Unknown),
    PivotGenerator(field, FieldType.Unknown),
  ]);
const widget = AggregationWidget.builder().newId().config(widgetConfig).build();

describe('ShowDocumentsHandler', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('adds a new message widget', () => {
    return ShowDocumentsHandler({ queryId, field, value: 42, type: FieldType.Unknown, contexts: { widget: widget, valuePath: [{ bar: 42 }, { [field]: 'Hello!' }] } })
      .then(() => {
        expect(WidgetActions.create).toHaveBeenCalled();
      });
  });

  it('adds a new message widget for an empty value path', () => {
    return ShowDocumentsHandler({ queryId, field, value: 42, type: FieldType.Unknown, contexts: { widget: widget, valuePath: [] } })
      .then(() => {
        expect(WidgetActions.create).toHaveBeenCalled();

        const newWidget = asMock(WidgetActions.create).mock.calls[0][0];

        expect(newWidget.query).toEqual(createElasticsearchQueryString());
      });
  });

  it('adds the given value path as widget filter for new message widget', () => {
    return ShowDocumentsHandler({ queryId, field, value: 42, type: FieldType.Unknown, contexts: { widget: widget, valuePath: [{ bar: 42 }, { [field]: 'Hello!' }] } })
      .then(() => {
        expect(WidgetActions.create).toHaveBeenCalled();

        const newWidget = asMock(WidgetActions.create).mock.calls[0][0];

        expect(newWidget.query).toEqual(createElasticsearchQueryString('foo:Hello\\! AND bar:42'));
      });
  });

  it('adds the given value path to an existing widget query', () => {
    const widgetWithFilter = widget.toBuilder().query(createElasticsearchQueryString('baz:23')).build();

    return ShowDocumentsHandler({ queryId, field, value: 42, type: FieldType.Unknown, contexts: { widget: widgetWithFilter, valuePath: [{ bar: 42 }, { [field]: 'Hello!' }] } })
      .then(() => {
        expect(WidgetActions.create).toHaveBeenCalled();

        const newWidget = asMock(WidgetActions.create).mock.calls[0][0];

        expect(newWidget.query).toEqual(createElasticsearchQueryString('baz:23 AND foo:Hello\\! AND bar:42'));
      });
  });

  it('sets title for new messages widget', () => {
    const widgetWithFilter = widget.toBuilder().query(createElasticsearchQueryString('foo:23')).build();

    return ShowDocumentsHandler({ queryId, field: 'hello', value: 'world', type: FieldType.Unknown, contexts: { widget: widgetWithFilter, valuePath: [{ bar: 42 }, { hello: 'world' }] } })
      .then(() => {
        const newWidget = asMock(WidgetActions.create).mock.calls[0][0];

        expect(TitlesActions.set).toHaveBeenCalledWith(TitleTypes.Widget, newWidget.id, 'Messages for hello:world AND bar:42');
      });
  });

  it('does not include duplicate source/timestamp fields twice', () => {
    const widgetWithFilter = widget.toBuilder().query(createElasticsearchQueryString('foo:23')).build();

    return ShowDocumentsHandler({
      queryId,
      field: 'hello',
      value: 'world',
      type: FieldType.Unknown,
      contexts: { widget: widgetWithFilter, valuePath: [{ timestamp: 'something' }, { source: 'hopper' }, { hello: 'world' }] },
    })
      .then(() => {
        const newWidget = asMock(WidgetActions.create).mock.calls[0][0];

        expect(newWidget.config.fields).toEqual(['timestamp', 'source', 'hello']);
      });
  });

  describe('on dashboard', () => {
    it('duplicates query/timerange/streams/filter of original widget', () => {
      const origWidget = Widget.builder()
        .filter('author: "Vanth"')
        .query(createElasticsearchQueryString('foo:42'))
        .streams(['stream1', 'stream23'])
        .timerange({ type: 'relative', range: 3600 })
        .build();

      return ShowDocumentsHandler({
        queryId,
        field: 'hello',
        value: 'world',
        type: FieldType.Unknown,
        contexts: {
          widget: origWidget,
          valuePath: [{ bar: 42 }, { hello: 'world' }],
        },
      }).then(() => {
        expect(WidgetActions.create).toHaveBeenCalled();

        const { filter, query, streams, timerange }: AggregationWidget = asMock(WidgetActions.create).mock.calls[0][0];

        expect(filter).toEqual('author: "Vanth"');
        expect(query).toEqual(createElasticsearchQueryString('foo:42 AND hello:world AND bar:42'));
        expect(streams).toEqual(['stream1', 'stream23']);
        expect(timerange).toEqual({ type: 'relative', range: 3600 });
      });
    });
  });
});

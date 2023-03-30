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
import FieldType from 'views/logic/fieldtypes/FieldType';
import { MISSING_BUCKET_NAME } from 'views/Constants';
import mockDispatch from 'views/test/mockDispatch';
import { createViewWithWidgets } from 'fixtures/searches';
import type { RootState } from 'views/types';
import { addWidget } from 'views/logic/slices/widgetActions';
import MessagesWidget from 'views/logic/widgets/MessagesWidget';
import { setTitle } from 'views/logic/slices/titlesActions';

import ShowDocumentsHandler from './ShowDocumentsHandler';

import AggregationWidget from '../aggregationbuilder/AggregationWidget';
import AggregationWidgetConfig from '../aggregationbuilder/AggregationWidgetConfig';
import PivotGenerator from '../searchtypes/aggregation/PivotGenerator';
import { createElasticsearchQueryString } from '../queries/Query';
import Widget from '../widgets/Widget';

const field = 'foo';

const widgetConfig = AggregationWidgetConfig
  .builder()
  .rowPivots([
    PivotGenerator('bar', FieldType.Unknown),
    PivotGenerator(field, FieldType.Unknown),
  ]);
const widget: Widget = AggregationWidget.builder().newId().config(widgetConfig).build();

jest.mock('views/logic/slices/widgetActions', () => ({
  addWidget: jest.fn(async () => {}),
}));

jest.mock('views/logic/slices/titlesActions', () => ({
  setTitle: jest.fn(async () => {}),
}));

describe('ShowDocumentsHandler', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  const view = createViewWithWidgets([widget], {});
  const dispatch = mockDispatch({ view: { view, activeQuery: 'query-id-1' } } as RootState);

  it('adds a new message widget', async () => {
    await dispatch(ShowDocumentsHandler({ contexts: { widget, valuePath: [{ bar: 42 }, { [field]: 'Hello!' }] } }));

    expect(addWidget).toHaveBeenCalledWith(expect.objectContaining({
      config: expect.objectContaining({ fields: ['timestamp', 'source', 'bar', 'foo'] }),
      type: MessagesWidget.type,
      query: expect.objectContaining({
        query_string: 'bar:42 AND foo:Hello\\!',
      }),
    }));

    expect(setTitle).toHaveBeenCalledWith('query-id-1', 'widget', expect.any(String), 'Messages for bar:42 AND foo:Hello\\!');
  });

  it('adds a new message widget for an empty value path', async () => {
    await dispatch(ShowDocumentsHandler({ contexts: { widget, valuePath: [] } }));

    expect(addWidget).toHaveBeenCalledWith(expect.objectContaining({
      config: expect.objectContaining({ fields: ['timestamp', 'source'] }),
      type: MessagesWidget.type,
      query: expect.objectContaining({
        query_string: '',
      }),
    }));
  });

  it('adds the given value path as widget filter for new message widget', async () => {
    await dispatch(ShowDocumentsHandler({ contexts: { widget, valuePath: [{ bar: 42 }, { [field]: 'Hello!' }] } }));

    expect(addWidget).toHaveBeenCalledWith(expect.objectContaining({
      config: expect.objectContaining({ fields: ['timestamp', 'source', 'bar', 'foo'] }),
      type: MessagesWidget.type,
      query: expect.objectContaining({
        query_string: 'bar:42 AND foo:Hello\\!',
      }),
    }));
  });

  it('adds the given value path to an existing widget query', async () => {
    const widgetWithFilter = widget.toBuilder().query(createElasticsearchQueryString('baz:23')).build();

    await dispatch(ShowDocumentsHandler({ contexts: { widget: widgetWithFilter, valuePath: [{ bar: 42 }, { [field]: 'Hello!' }] } }));

    expect(addWidget).toHaveBeenCalledWith(expect.objectContaining({
      config: expect.objectContaining({ fields: ['timestamp', 'source', 'bar', 'foo'] }),
      type: MessagesWidget.type,
      query: expect.objectContaining({
        query_string: 'baz:23 AND bar:42 AND foo:Hello\\!',
      }),
    }));
  });

  it('sets title for new messages widget', async () => {
    const widgetWithFilter = widget.toBuilder().query(createElasticsearchQueryString('foo:23')).build();

    await dispatch(ShowDocumentsHandler({ contexts: { widget: widgetWithFilter, valuePath: [{ bar: 42 }, { hello: 'world' }] } }));

    expect(setTitle).toHaveBeenCalledWith('query-id-1', 'widget', expect.any(String), 'Messages for bar:42 AND hello:world');
  });

  it('does not include duplicate source/timestamp fields twice', async () => {
    const widgetWithFilter = widget.toBuilder().query(createElasticsearchQueryString('foo:23')).build();

    await dispatch(ShowDocumentsHandler({
      contexts: { widget: widgetWithFilter, valuePath: [{ timestamp: 'something' }, { source: 'hopper' }, { hello: 'world' }] },
    }));

    expect(addWidget).toHaveBeenCalledWith(expect.objectContaining({
      config: expect.objectContaining({ fields: ['timestamp', 'source', 'hello'] }),
    }));
  });

  it('creates correct widget query for missing bucket field value', async () => {
    await dispatch(ShowDocumentsHandler({ contexts: { widget, valuePath: [{ foo: MISSING_BUCKET_NAME }] } }));

    expect(addWidget).toHaveBeenCalledWith(expect.objectContaining({
      query: expect.objectContaining({
        query_string: 'NOT _exists_:foo',
      }),
    }));
  });

  describe('on dashboard', () => {
    it('duplicates query/timerange/streams/filter of original widget', async () => {
      const origWidget = Widget.builder()
        .filter('author: "Vanth"')
        .query(createElasticsearchQueryString('foo:42'))
        .streams(['stream1', 'stream23'])
        .timerange({ type: 'relative', range: 3600 })
        .build();

      const dashboardView = createViewWithWidgets([origWidget], {});
      const _dispatch = mockDispatch({ view: { view: dashboardView, activeQuery: 'query-id-1' } } as RootState);

      await _dispatch(ShowDocumentsHandler({
        contexts: {
          widget: origWidget,
          valuePath: [{ bar: 42 }, { hello: 'world' }],
        },
      }));

      expect(addWidget).toHaveBeenCalledWith(expect.objectContaining({
        filter: 'author: "Vanth"',
        streams: ['stream1', 'stream23'],
        timerange: { type: 'relative', range: 3600 },
        query: expect.objectContaining({ query_string: 'foo:42 AND bar:42 AND hello:world' }),
      }));
    });
  });
});

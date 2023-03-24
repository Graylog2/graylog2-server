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
import pivotForField from 'views/logic/searchtypes/aggregation/PivotGenerator';
import Series from 'views/logic/aggregationbuilder/Series';
import { createSearch } from 'fixtures/searches';
import mockDispatch from 'views/test/mockDispatch';
import type { RootState } from 'views/types';
import { addWidget } from 'views/logic/slices/widgetActions';

import ChartActionHandler from './ChartActionHandler';

import FieldType, { FieldTypes } from '../fieldtypes/FieldType';
import { createElasticsearchQueryString } from '../queries/Query';

jest.mock('views/logic/searchtypes/aggregation/PivotGenerator', () => jest.fn());

jest.mock('views/logic/slices/widgetActions', () => ({
  addWidget: jest.fn(),
}));

describe('ChartActionHandler', () => {
  const emptyFieldType = new FieldType('empty', [], []);
  const view = createSearch();
  const dispatch = mockDispatch({ view: { view, activeQuery: 'query-id-1' } } as RootState);

  it('uses average function if triggered on field', async () => {
    await dispatch(ChartActionHandler({ queryId: 'queryId', field: 'somefield', type: emptyFieldType, contexts: {} }));

    expect(addWidget).toHaveBeenCalledWith(expect.objectContaining({
      config: expect.objectContaining({
        series: [Series.forFunction('avg(somefield)')],
      }),
    }));
  });

  it('uses the function itself if it was triggered on one', async () => {
    await dispatch(ChartActionHandler({ queryId: 'queryId', field: 'max(somefield)', type: emptyFieldType, contexts: {} }));

    expect(addWidget).toHaveBeenCalledWith(expect.objectContaining({
      config: expect.objectContaining({
        series: [Series.forFunction('max(somefield)')],
      }),
    }));
  });

  describe('Widget creation', () => {
    it('should create widget with filter of original widget', async () => {
      const filter = "author: 'Vanth'";
      const origWidget = Widget.builder().filter(filter).build();
      const timestampFieldType = FieldTypes.DATE();

      await dispatch(ChartActionHandler({ queryId: 'queryId', field: 'somefield', type: emptyFieldType, contexts: { widget: origWidget } }));

      expect(addWidget).toHaveBeenCalledWith(expect.objectContaining({
        filter,
      }));

      expect(pivotForField).toHaveBeenCalledWith('timestamp', timestampFieldType);
    });

    it('duplicates query/timerange/streams/filter of original widget', async () => {
      const origWidget = Widget.builder()
        .filter('author: "Vanth"')
        .query(createElasticsearchQueryString('foo:42'))
        .streams(['stream1', 'stream23'])
        .timerange({ type: 'relative', range: 3600 })
        .build();

      await dispatch(ChartActionHandler({
        queryId: 'queryId',
        field: 'foo',
        type: new FieldType('keyword', [], []),
        contexts: { widget: origWidget },
      }));

      expect(addWidget).toHaveBeenCalledWith(expect.objectContaining({
        filter: 'author: "Vanth"',
        query: createElasticsearchQueryString('foo:42'),
        streams: ['stream1', 'stream23'],
        timerange: { type: 'relative', range: 3600 },
      }));
    });
  });
});

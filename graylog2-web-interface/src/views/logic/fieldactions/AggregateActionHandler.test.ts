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
import { addWidget } from 'views/logic/slices/widgetActions';
import { createSearch } from 'fixtures/searches';
import mockDispatch from 'views/test/mockDispatch';
import type { RootState } from 'views/types';

import AggregateActionHandler from './AggregateActionHandler';

import FieldType from '../fieldtypes/FieldType';
import Pivot from '../aggregationbuilder/Pivot';
import Widget from '../widgets/Widget';
import { createElasticsearchQueryString } from '../queries/Query';

jest.mock('views/components/datatable/DataTable', () => ({ type: 'table' }));

jest.mock('views/logic/slices/widgetActions', () => ({
  addWidget: jest.fn(),
}));

describe('AggregateActionHandler', () => {
  const view = createSearch();
  const dispatch = mockDispatch({ view: { view, activeQuery: 'query-id-1' } } as RootState);

  it('uses field type when generating widget', async () => {
    await dispatch(AggregateActionHandler({ queryId: 'queryId', field: 'foo', type: new FieldType('keyword', [], []), contexts: {} }));

    expect(addWidget).toHaveBeenCalledWith(expect.objectContaining({
      config: expect.objectContaining({
        rowPivots: [
          Pivot.createValues(['foo']),
        ],
      }),
    }));
  });

  it('uses filter from widget', async () => {
    const filter = 'author: "Vanth"';
    const origWidget = Widget.builder().filter(filter).build();

    await dispatch(AggregateActionHandler({ queryId: 'queryId', field: 'foo', type: new FieldType('keyword', [], []), contexts: { widget: origWidget } }));

    expect(addWidget).toHaveBeenCalledWith(expect.objectContaining({
      filter,
    }));
  });

  it('duplicates query/timerange/streams/filter of original widget', async () => {
    const origWidget = Widget.builder()
      .filter('author: "Vanth"')
      .query(createElasticsearchQueryString('foo:42'))
      .streams(['stream1', 'stream23'])
      .timerange({ type: 'relative', range: 3600 })
      .build();

    await dispatch(AggregateActionHandler({
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

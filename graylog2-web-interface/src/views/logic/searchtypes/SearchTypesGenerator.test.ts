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
import * as Immutable from 'immutable';
import asMock from 'helpers/mocking/AsMock';

import { createWidget } from 'views/logic/WidgetTestHelpers';

import SearchTypesGenerator from './SearchTypesGenerator';

import { widgetDefinition } from '../Widgets';
import Widget from '../widgets/Widget';

jest.mock('../Widgets', () => ({
  widgetDefinition: jest.fn(() => ({ searchTypes: () => [{}] })),
}));

jest.mock('../SearchType', () => () => ({ defaults: {} }));

const dummyWidget = new Widget('dummyWidget', 'dummy', {});

const mockSearchType = (fn) => asMock(widgetDefinition).mockImplementation((type) => ({
  dummy: {
    ...createWidget(type),
    searchTypes: fn,
  },
}[type]));

describe('SearchTypesGenerator', () => {
  it('should include filters of widgets', () => {
    const widgetWithoutFilter = new Widget('widgetWithoutFilter', 'mock', {});

    const widgetWithFilter = widgetWithoutFilter.toBuilder()
      .id('widgetWithFilter')
      .filter('source: foo')
      .build();

    const widgets = [widgetWithoutFilter, widgetWithFilter];

    const { searchTypes, widgetMapping } = SearchTypesGenerator(widgets);

    expect(Object.keys(widgetMapping.toJS())).toEqual(['widgetWithoutFilter', 'widgetWithFilter']);

    const widgetWithFilterId = widgetMapping.get('widgetWithFilter').first();
    const widgetWithoutFilterId = widgetMapping.get('widgetWithoutFilter').first();

    const searchTypeWithFilter = searchTypes.find((w) => (w.id === widgetWithFilterId));
    const searchTypeWithoutFilter = searchTypes.find((w) => (w.id === widgetWithoutFilterId));

    expect(searchTypeWithFilter.filter).toEqual({ query: 'source: foo', type: 'query_string' });
    expect(searchTypeWithoutFilter.filter).toBeUndefined();
  });

  it('allows search type to override timerange', () => {
    const widgetWithTimerange = dummyWidget.toBuilder()
      .timerange({ type: 'relative', from: 300 })
      .build();

    mockSearchType(() => ([{ timerange: { type: 'keyword', keyword: 'yesterday' } }]));

    const { searchTypes, widgetMapping } = SearchTypesGenerator([widgetWithTimerange]);

    const searchType = searchTypes[0];

    expect(searchType.timerange).toEqual({ type: 'keyword', keyword: 'yesterday' });
    expect(widgetMapping.get('dummyWidget')).toEqual(Immutable.Set([searchType.id]));
  });

  it('allows search type to override id', () => {
    mockSearchType(() => ([{ id: 'bar' }]));

    const { searchTypes, widgetMapping } = SearchTypesGenerator([dummyWidget]);

    const searchType = searchTypes[0];

    expect(searchType.id).toEqual('bar');
    expect(widgetMapping.get('dummyWidget')).toEqual(Immutable.Set([searchType.id]));
  });

  it('allows search type to override query', () => {
    const widgetWithTimerange = dummyWidget.toBuilder()
      .query({ type: 'elasticsearch', query_string: '_exists_:src_ip' })
      .build();

    mockSearchType((widget) => ([{ query: { type: 'elasticsearch', query_string: `${widget.query.query_string} AND source:foo` } }]));

    const { searchTypes, widgetMapping } = SearchTypesGenerator([widgetWithTimerange]);

    const searchType = searchTypes[0];

    expect(searchType.query).toEqual({ type: 'elasticsearch', query_string: '_exists_:src_ip AND source:foo' });
    expect(widgetMapping.get('dummyWidget')).toEqual(Immutable.Set([searchType.id]));
  });
});

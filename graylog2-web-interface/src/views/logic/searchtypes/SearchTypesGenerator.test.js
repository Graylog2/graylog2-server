// @flow strict
import * as Immutable from 'immutable';
import asMock from 'helpers/mocking/AsMock';
import SearchTypesGenerator from './SearchTypesGenerator';
import { widgetDefinition } from '../Widgets';
import Widget from '../widgets/Widget';

jest.mock('../Widgets', () => ({
  widgetDefinition: jest.fn(() => ({ searchTypes: () => [{}] })),
}));

jest.mock('../SearchType', () => () => ({ defaults: {} }));

const dummyWidget = new Widget('dummyWidget', 'dummy', {});

const mockSearchType = fn => asMock(widgetDefinition).mockImplementation(type => ({
  dummy: {
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

    const searchTypeWithFilter = searchTypes.find(w => (w.get('id') === widgetWithFilterId), null, Immutable.Map()).toJS();
    const searchTypeWithoutFilter = searchTypes.find(w => (w.get('id') === widgetWithoutFilterId), null, Immutable.Map()).toJS();

    expect(searchTypeWithFilter.filter).toEqual({ query: 'source: foo', type: 'query_string' });
    expect(searchTypeWithoutFilter.filter).toBeUndefined();
  });
  it('allows search type to override timerange', () => {
    const widgetWithTimerange = dummyWidget.toBuilder()
      .timerange({ type: 'relative', range: 300 })
      .build();
    mockSearchType(() => ([{ timerange: { type: 'keyword', keyword: 'yesterday' } }]));

    const { searchTypes, widgetMapping } = SearchTypesGenerator([widgetWithTimerange]);

    const searchType = searchTypes.first();
    expect(searchType.get('timerange')).toEqual(Immutable.Map({ type: 'keyword', keyword: 'yesterday' }));
    expect(widgetMapping.get('dummyWidget')).toEqual(Immutable.Set([searchType.get('id')]));
  });
  it('allows search type to override id', () => {
    mockSearchType(() => ([{ id: 'bar' }]));

    const { searchTypes, widgetMapping } = SearchTypesGenerator([dummyWidget]);

    const searchType = searchTypes.first();
    expect(searchType.get('id')).toEqual('bar');
    expect(widgetMapping.get('dummyWidget')).toEqual(Immutable.Set([searchType.get('id')]));
  });
  it('allows search type to override query', () => {
    const widgetWithTimerange = dummyWidget.toBuilder()
      .query({ type: 'elasticsearch', query_string: '_exists_:src_ip' })
      .build();
    mockSearchType(widget => ([{ query: { type: 'elasticsearch', query_string: `${widget.query.query_string} AND source:foo` } }]));

    const { searchTypes, widgetMapping } = SearchTypesGenerator([widgetWithTimerange]);

    const searchType = searchTypes.first();
    expect(searchType.get('query')).toEqual(Immutable.Map({ type: 'elasticsearch', query_string: '_exists_:src_ip AND source:foo' }));
    expect(widgetMapping.get('dummyWidget')).toEqual(Immutable.Set([searchType.get('id')]));
  });
});

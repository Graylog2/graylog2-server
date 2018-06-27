import SearchTypesGenerator from './SearchTypesGenerator';
import Widget from '../widgets/Widget';

jest.mock('../Widget', () => ({
  widgetDefinition: () => ({ searchTypes: () => [{}] }),
}));

jest.mock('../SearchType', () => ({
  searchTypeDefinition: () => ({ defaults: {} }),
}));

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

    const searchTypeWithFilter = searchTypes.find(w => (w.get('id') === widgetWithFilterId)).toJS();
    const searchTypeWithoutFilter = searchTypes.find(w => (w.get('id') === widgetWithoutFilterId)).toJS();

    expect(searchTypeWithFilter.filter).toEqual({ query: 'source: foo', type: 'query_string' });
    expect(searchTypeWithoutFilter.filter).toBeUndefined();
  });
});

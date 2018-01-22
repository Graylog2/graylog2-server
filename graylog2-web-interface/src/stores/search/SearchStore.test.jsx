import StoreProvider from 'injection/StoreProvider';

const SearchStore = StoreProvider.getStore('Search');

describe('SearchStore', () => {
  beforeEach(() => {
    SearchStore.query = '';
  });

  it('should add a query', () => {
    SearchStore.addSearchTerm('field', 'value');
    expect(SearchStore.query).toEqual('field:value');
  });

  it('should append a new query with "AND"', () => {
    SearchStore.addSearchTerm('field1', 'value1');
    SearchStore.addSearchTerm('field2', 'value2');
    expect(SearchStore.query).toEqual('field1:value1 AND field2:value2');
  });

  describe('escaping', () => {
    it('should escape a query with spaces and backslashes', () => {
      SearchStore.addSearchTerm('field', '&& || : \\ / + - ! ( ) { } [ ] ^ " ~ * ?');
      expect(SearchStore.query).toEqual('field:"&& || : \\\\ / + - ! ( ) { } [ ] ^ \\" ~ * ?"');
    });

    it('should escape a query with spaces and backslashes like Windows File Path', () => {
      SearchStore.addSearchTerm('field', 'C:\\Program Files\\Atlassian\\Application Data\\Graylog\\log\\some.log');
      expect(SearchStore.query).toEqual('field:"C:\\\\Program Files\\\\Atlassian\\\\Application Data\\\\Graylog\\\\log\\\\some.log"');
    });

    it('should escape a query with special chars and no spaces', () => {
      SearchStore.addSearchTerm('field', '&&||:\\/+-!(){}[]^"~*?');
      expect(SearchStore.query).toEqual('field:\\&&\\||\\:\\\\\\/\\+\\-\\!\\(\\)\\{\\}\\[\\]\\^\\"\\~\\*\\?');
    });
  });
});

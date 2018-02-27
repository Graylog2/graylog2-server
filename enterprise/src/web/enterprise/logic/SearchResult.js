import _ from 'lodash';

import { searchTypeDefinition } from 'enterprise/logic/SearchType';

const _searchTypePlugin = (type) => {
  return searchTypeDefinition(type).handler ||
    {
      convert: (result) => {
        console.log(`No search type handler for type '${type}' result:`, result);
        return result;
      },
    };
};

class SearchResult {
  constructor(searchRequest, result) {
    this.searchRequest = searchRequest;
    this.result = result;

    this.results = _.mapValues(result.results, (queryResult) => {
      return {
        query: queryResult.query,
        searchTypes: _.mapValues(queryResult.search_types, (searchType) => {
          // each search type has a custom data structure attached to it, let the plugin convert the value
          return _searchTypePlugin(searchType.type).convert(searchType);
        }),
      };
    });
  }

  forId(queryId) {
    return this.results[queryId];
  }

  widgetMapping() {
    return this.searchRequest.getWidgetMapping();
  }
}

export default SearchResult;

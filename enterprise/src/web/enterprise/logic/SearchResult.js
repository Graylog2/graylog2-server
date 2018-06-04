import _ from 'lodash';

import { searchTypeDefinition } from 'enterprise/logic/SearchType';

const _searchTypePlugin = (type) => {
  const typeDefinition = searchTypeDefinition(type);
  return typeDefinition && typeDefinition.handler ? searchTypeDefinition(type).handler :
    {
      convert: (result) => {
        console.log(`No search type handler for type '${type}' result:`, result);
        return result;
      },
    };
};

class SearchResult {
  constructor(result) {
    this.result = result;

    this.results = _.mapValues(result.results, (queryResult) => {
      // eslint-disable-next-line camelcase
      const { duration, timestamp, effective_timerange } = queryResult.execution_stats;
      return {
        query: queryResult.query,
        duration,
        timestamp,
        effectiveTimerange: effective_timerange,
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
}

export default SearchResult;

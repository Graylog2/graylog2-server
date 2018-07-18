import _ from 'lodash';
import QueryResult from './QueryResult';

class SearchResult {
  constructor(result) {
    this.result = result;

    this.results = _.mapValues(result.results, (queryResult) => new QueryResult(queryResult));
  }

  forId(queryId) {
    return this.results[queryId];
  }
}

export default SearchResult;

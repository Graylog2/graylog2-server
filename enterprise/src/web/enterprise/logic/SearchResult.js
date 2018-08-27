import _ from 'lodash';
import QueryResult from './QueryResult';
import SearchError from './SearchError';

class SearchResult {
  constructor(result) {
    this.result = result;

    this.results = _.mapValues(result.results, queryResult => new QueryResult(queryResult));
    this.errors = _.get(result, 'errors', []).map(error => new SearchError(error));
  }

  forId(queryId) {
    return this.results[queryId];
  }
}

export default SearchResult;

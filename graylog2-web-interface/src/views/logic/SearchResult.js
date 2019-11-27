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

  updateSearchTypes(searchTypeResults) {
    const updatedResult = this.result;
    _.forEach(searchTypeResults, (searchTypeResult) => {
      const searchTypeId = searchTypeResult.id;
      const searchQuery = Object.values(this.result.results).find(query => query.search_types[searchTypeId]);
      updatedResult.results[searchQuery.query.id].search_types[searchTypeId] = searchTypeResult;
    });
    return new SearchResult(updatedResult);
  }
}

export default SearchResult;

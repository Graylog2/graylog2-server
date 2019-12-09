import { fromJS } from 'immutable';
import { mapValues, get, compact } from 'lodash';
import QueryResult from './QueryResult';
import SearchError from './SearchError';
import ResultWindowLimitError from './ResultWindowLimitError';

class SearchResult {
  constructor(result) {
    this._result = fromJS(result);

    this._results = fromJS(mapValues(result.results, queryResult => new QueryResult(queryResult)));
    this._errors = fromJS(get(result, 'errors', []).map((error) => {
      if (error.type === 'result_window_limit') {
        return new ResultWindowLimitError(error, this);
      }
      return new SearchError(error);
    }));
  }

  get result() {
    return this._result.toJS();
  }

  get results() {
    return this._results.toJS();
  }

  get errors() {
    return this._errors.toJS();
  }

  forId(queryId) {
    return this._results.get(queryId);
  }

  updateSearchTypes(searchTypeResults) {
    const updatedResult = this.result;
    searchTypeResults.forEach((searchTypeResult) => {
      const searchQuery = this._getQueryBySearchTypeId(searchTypeResult.id);
      updatedResult.results[searchQuery.query.id].search_types[searchTypeResult.id] = searchTypeResult;
    });
    return new SearchResult(updatedResult);
  }

  getSearchTypesFromResponse(searchTypeIds) {
    const searchTypes = searchTypeIds.map((searchTypeId) => {
      const relatedQuery = this._getQueryBySearchTypeId(searchTypeId);
      return SearchResult._getSearchTypeFromQuery(relatedQuery, searchTypeId);
    });
    return SearchResult._filterFailedSearchTypes(searchTypes);
  }

  _getQueryBySearchTypeId(searchTypeId) {
    return Object.values(this.result.results).find(query => SearchResult._getSearchTypeFromQuery(query, searchTypeId));
  }

  static _getSearchTypeFromQuery(query, searchTypeId) {
    return (query && query.search_types) ? query.search_types[searchTypeId] : undefined;
  }

  static _filterFailedSearchTypes(searchTypes) {
    return compact(searchTypes);
  }
}

export default SearchResult;

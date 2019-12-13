import SearchError from './SearchError';

export default class ResultWindowLimitError extends SearchError {
  constructor(error, result) {
    super(error);
    const { result_window_limit: resultWindowLimit } = error;
    this._state = {
      ...this._state,
      description: ResultWindowLimitError._extendDescription(result, this.description, this.queryId, this.searchTypeId, resultWindowLimit),
      result_window_limit: resultWindowLimit,
    };
  }

  static _extendDescription(result, description, queryId, searchTypeId, resultWindowLimit) {
    const pageSize = ResultWindowLimitError._getPageSizeFromResult(result, queryId, searchTypeId);
    const validPages = Math.floor(resultWindowLimit / pageSize);
    const validPagesMessage = `Elasticsearch limits the search result to ${resultWindowLimit} messages. With a page size of ${pageSize} messages, you can use the first ${validPages} pages.`;
    return `${validPagesMessage} ${description}`;
  }

  static _getPageSizeFromResult(result, queryId, searchTypeId) {
    const searchTypes = result.results[queryId].query.search_types;
    const searchType = searchTypes.find(({ id }) => id === searchTypeId);
    return searchType.limit;
  }

  get resultWindowLimit() { return this._state.result_window_limit; }
}

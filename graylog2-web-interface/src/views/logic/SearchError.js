export default class SearchError {
  constructor(error) {
    // eslint-disable-next-line camelcase
    const { backtrace, description, query_id, search_type_id, result_window_limit, type } = error;
    this._state = {
      backtrace,
      description,
      query_id,
      result_window_limit,
      search_type_id,
      type,
    };
  }

  get backtrace() { return this._state.backtrace; }

  get description() { return this._state.description; }

  get queryId() { return this._state.query_id; }

  get searchTypeId() { return this._state.search_type_id; }

  get resultWindowLimit() { return this._state.result_window_limit; }

  get type() { return this._state.type; }
}

import Reflux from 'reflux';
import Immutable from 'immutable';

import SearchActions from 'enterprise/actions/SearchActions';

const _defaultQuery = () => {
  return {
    query: '',
    rangeType: 'relative',
    rangeParams: Immutable.Map({ range: '300' }),
    fields: Immutable.Set.of('source', 'message'),
  };
};

/**
 * Contains the state for the search/view page components.
 * The server side of the query creation and execution is handled by the QueriesStore.
 */
export default Reflux.createStore({
  listenables: [SearchActions],

  state: {},

  getInitialState() {
    return {
      selectedQuery: 0,
      queries: Immutable.List().push(_defaultQuery()),
      currentJobId: null,
    };
  },

  query(query) {
    this.state.query = query;
    this._trigger();
  },

  rangeParams(key, value) {
    const current = this.state.queries[this.state.selectedQuery];
    current.rangeParams = current.set(key, value);
    this._trigger();
  },

  rangeType(rangeType) {
    const current = this.state.queries[this.state.selectedQuery];

    current.rangeParams = new Immutable.Map();
    current.rangeType = rangeType;
    this._trigger();
  },

  toggleField(field) {
    if (this.state.fields.contains(field)) {
      this.removeField(field);
    } else {
      this.addField(field);
    }
  },

  addField(field) {
    this.state.fields = this.state.fields.add(field);
    this._trigger();
  },

  removeField(field) {
    this.state.fields = this.state.fields.delete(field);
    this._trigger();
  },

  createRootQuery() {
    this.state.queries = this.state.queries.push(_defaultQuery());
    this._trigger();
    SearchActions.createRootQuery.promise(Promise.resolve(this.state.queries.size() - 1));
  },

  trackQueryJob(jobId) {
    this.state.currentJobId = jobId;
    this._trigger();
  },

  _trigger() {
    this.state.fullQuery = this._generateFullQuery(this.state.queries[0]);
    this.trigger(this.state);
  },

  _generateFullQuery(search) {
    return {
      timerange: Object.assign({
        type: search.rangeType,
      }, search.rangeParams.toObject()),
      query: {
        type: 'elasticsearch',
        query_string: search.query || '*',
      },
      search_types: [
        {
          id: 'messages',
          type: 'messages',
          limit: 150,
          offset: 0,
          sort: [{ field: 'timestamp', order: 'DESC' }, { field: 'source', order: 'ASC' }],
        },
        {
          id: 'histogram',
          type: 'date_histogram',
          interval: 'MINUTE',
        },
      ],
    };
  },
});

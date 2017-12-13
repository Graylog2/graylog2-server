import Reflux from 'reflux';
import Immutable from 'immutable';
import uuidv4 from 'uuid/v4';
import _ from 'lodash';
import { PluginStore } from 'graylog-web-plugin/plugin';

import ViewActions from 'enterprise/actions/ViewActions';
import SearchJobActions from 'enterprise/actions/SearchJobActions';
import SearchJobStore from './SearchJobStore';

const _defaultQuery = (id) => {
  return {
    id: id,
    query: '',
    rangeType: 'relative',
    rangeParams: Immutable.Map({ range: '300' }),
    fields: Immutable.Set.of('source', 'message'),
  };
};

/**
 * Contains the state for the search/view page components.
 * The server side of the query creation and execution is handled by the SearchJobStore.
 *
 * Essentially the view store deals with a single 'Search' from the server plus any view related state it needs.
 */
export default Reflux.createStore({
  listenables: [ViewActions],

  plugins: null,
  state: {},
  trackTimeout: null,

  init() {
    console.log('ViewStore init');
    this.state = this.getInitialState();
    const defaultQuery = _defaultQuery(uuidv4());
    this.state.selectedQuery = defaultQuery.id;
    this.state.queries = Immutable.OrderedMap().set(defaultQuery.id, defaultQuery);
    this._trigger();
    this.listenTo(SearchJobStore, this.onSearchJobsUpdate);
  },

  getInitialState() {
    return this.state;
  },

  onSearchJobsUpdate(data) {
    const job = data.jobs[this.state.currentJobId];
    if (job && !job.done) {
      this.trackTimeout = setTimeout(() => SearchJobActions.jobStatus(this.state.currentJobId), 250);
    }
    if (job) {
      const viewJob = {
        id: job.id,
        searchId: job.searchId,
      };
      // transform the raw search job from the API to something that we can use more easily for updating the views
      // results has the following form:
      /*
       {
        '4fa40944-ab6f-40bc-80ae-8e9398a1c57e': {
          query: { id: '4fa40944-ab6f-40bc-80ae-8e9398a1c57e', query info... },
          searchTypes: {
            '6c84fb90-12c4-11e1-840d-7b25c5ee775a': { type: 'messages', id: '6c84fb90-12c4-11e1-840d-7b25c5ee775a', search type data... },
            ...
          }
        }
       }
      */
      viewJob.results = _.mapValues(job.results, (queryResult) => {
        return {
          query: queryResult.query,
          searchTypes: _.mapValues(queryResult.search_types, (searchType) => {
            // each search type has a custom data structure attached to it, let the plugin convert the value
            return this.searchTypePlugin(searchType.type).convert(searchType);
          }),
        };
      });

      this.state.currentJob = viewJob;
    } else {
      this.state.currentJob = null;
    }


    this._trigger();
  },

  searchTypePlugin(type) {
    const plugins = {};
    PluginStore.exports('searchTypes').forEach((plugin) => {
      plugins[plugin.type] = plugin.handler;
    });
    return plugins[type] ||
      {
        convert: (result) => {
          console.log(`No search type handler for type '${type}' result:`, result);
          return result;
        },
      };
  },

  query(query) {
    const currentQuery = this._currentQuery();
    currentQuery.query = query;
    this._trigger();
  },

  rangeParams(key, value) {
    const current = this._currentQuery();
    current.rangeParams = current.rangeParams.set(key, value);
    this._trigger();
  },

  rangeType(rangeType) {
    const current = this._currentQuery();

    current.rangeParams = new Immutable.Map();
    current.rangeType = rangeType;
    this._trigger();
  },

  toggleField(field) {
    if (this._currentQuery().fields.contains(field)) {
      this.removeField(field);
    } else {
      this.addField(field);
    }
  },

  addField(field) {
    const current = this._currentQuery();
    current.fields = current.fields.add(field);
    this._trigger();
  },

  removeField(field) {
    const current = this._currentQuery();
    current.fields = current.fields.delete(field);
    this._trigger();
  },

  createRootQuery() {
    const newQuery = _defaultQuery(uuidv4());
    this.state.queries = this.state.queries.set(newQuery.id, newQuery);
    this.state.selectedQuery = newQuery.id;
    this._trigger();
  },

  removeRootQuery(queryId) {
    if (this.state.queries.size === 1) {
      // cannot remove the last query
      return;
    }

    this.state.queries = this.state.queries.delete(queryId);
    // TODO pick the neighboring query, instead of being lazy
    this.state.selectedQuery = this.state.queries.first().id;
    this._trigger();
  },

  trackQueryJob(jobId) {
    this.state.currentJobId = jobId;
    SearchJobActions.jobStatus(jobId);
    this._trigger();
    return jobId;
  },

  selectQuery(queryId) {
    console.log(`Selected query was ${this.state.selectedQuery}, now ${queryId}`);
    this.state.selectedQuery = queryId;
    this._trigger();
  },

  _currentQuery() {
    return this.state.queries.get(this.state.selectedQuery);
  },

  _trigger() {
    this.trigger(this.state);
  },
});

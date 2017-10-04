import Reflux from 'reflux';
import Immutable from 'immutable';

import SearchActions from 'enterprise/actions/SearchActions';

export default Reflux.createStore({
  listenables: [SearchActions],
  state: {
    query: '',
    rangeType: 'relative',
    rangeParams: Immutable.Map({ relative: '300' }),
    fields: Immutable.Set.of('source', 'message'),
  },
  getInitialState() {
    this.state.fullQuery = this._generateFullQuery(this.state);
    return this.state;
  },
  query(query) {
    this.state.query = query;
    this._trigger();
  },
  rangeParams(key, value) {
    this.state.rangeParams = this.state.rangeParams.set(key, value);
    this._trigger();
  },
  rangeType(rangeType) {
    this.state.rangeType = rangeType;
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
  _trigger() {
    this.state.fullQuery = this._generateFullQuery(this.state);
    this.trigger(this.state);
  },

  _generateFullQuery(search) {
    return {
      timerange: Object.assign({
        type: search.rangeType,
      }, search.rangeParams.toObject()),
      query: {
        elasticsearch: search.query || '*',
      },
      search_types: [

      ],
    };
  },
});
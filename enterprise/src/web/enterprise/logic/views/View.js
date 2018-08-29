import Immutable from 'immutable';
import ObjectID from 'bson-objectid';

import ViewState from './ViewState';
import DashboardState from './DashboardState';

export default class View {
  constructor(id, title, summary, description, search, properties, state, dashboardState, createdAt) {
    this._value = { id, title, summary, description, search, properties: Immutable.fromJS(properties), state: Immutable.fromJS(state), dashboardState, createdAt };
  }

  static create() {
    return new View(ObjectID().toString(), undefined, undefined, undefined, undefined, undefined, ViewState.create(), DashboardState.create(), new Date());
  }

  get id() {
    return this._value.id;
  }

  get title() {
    return this._value.title;
  }

  get summary() {
    return this._value.summary;
  }

  get description() {
    return this._value.description;
  }

  get search() {
    return this._value.search;
  }

  get properties() {
    return this._value.properties;
  }

  get state() {
    return this._value.state;
  }

  get dashboardState() {
    return this._value.dashboardState;
  }

  get createdAt() {
    return this._value.createdAt;
  }

  get widgetMapping() {
    return (this.state || Immutable.Map()).valueSeq().map(s => s.widgetMapping).reduce((prev, cur) => Immutable.fromJS(prev).merge(Immutable.fromJS(cur)));
  }

  toBuilder() {
    const { id, title, summary, description, search, properties, state, dashboardState, createdAt } = this._value;
    // eslint-disable-next-line no-use-before-define
    return new Builder(Immutable.Map({
      id,
      title,
      summary,
      description,
      search,
      properties,
      state,
      dashboardState,
      createdAt,
    }));
  }

  toJSON() {
    const { id, title, summary, description, search, properties, state, dashboardState, createdAt } = this._value;

    return {
      id,
      title,
      summary,
      description,
      search_id: search.id,
      properties,
      state,
      dashboard_state: dashboardState,
      created_at: createdAt,
    };
  }

  static fromJSON(value) {
    // eslint-disable-next-line camelcase
    const { id, title, summary, description, search, properties, state, dashboard_state, created_at } = value;
    const viewState = Immutable.Map(state).map(ViewState.fromJSON);
    const dashboardState = DashboardState.fromJSON(dashboard_state);
    return new View(
      id,
      title,
      summary,
      description,
      search,
      properties,
      viewState,
      dashboardState,
      created_at,
    );
  }
}

class Builder {
  constructor(value = Immutable.Map()) {
    this.value = value;
  }

  id(value) {
    return new Builder(this.value.set('id', value));
  }

  newId() {
    return this.id(ObjectID().toString());
  }

  title(value) {
    return new Builder(this.value.set('title', value));
  }

  summary(value) {
    return new Builder(this.value.set('summary', value));
  }

  description(value) {
    return new Builder(this.value.set('description', value));
  }

  search(value) {
    return new Builder(this.value.set('search', value));
  }

  properties(value) {
    return new Builder(this.value.set('properties', value));
  }

  state(value) {
    return new Builder(this.value.set('state', Immutable.fromJS(value)));
  }

  dashboardState(value) {
    return new Builder(this.value.set('dashboardState', value));
  }

  createdAt(value) {
    return new Builder(this.value.set('createdAt', value));
  }

  build() {
    const { id, title, summary, description, search, properties, state, dashboardState, createdAt } = this.value.toObject();
    return new View(id, title, summary, description, search, properties, state, dashboardState, createdAt);
  }
}

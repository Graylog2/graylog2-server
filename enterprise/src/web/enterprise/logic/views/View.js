// @flow strict
import * as Immutable from 'immutable';
import ObjectID from 'bson-objectid';

import ViewState from './ViewState';
import DashboardState from './DashboardState';
import Search from '../search/Search';
import type { QueryId } from '../queries/Query';

export type Properties = Immutable.List<any>;

type InternalState = {
  id: string,
  title: string,
  summary: string,
  description: string,
  search: Search,
  properties: Properties,
  state: Immutable.Map<QueryId, ViewState>,
  dashboardState: DashboardState,
  createdAt: Date,
  owner: string,
};

export type WidgetMapping = Immutable.Map<string, string>;
export type ViewJson = {
  id: string,
  title: string,
  summary: string,
  description: string,
  search_id: string,
  properties: Properties,
  state: { [QueryId]: ViewState },
  dashboard_state: any,
  created_at: Date,
  owner: string,
};

export default class View {
  _value: InternalState;

  constructor(id: string,
    title: string,
    summary: string,
    description: string,
    search: Search,
    properties: Properties,
    state: Immutable.Map<QueryId, ViewState>,
    dashboardState: DashboardState,
    createdAt: Date, owner: string) {
    this._value = {
      id,
      title,
      summary,
      description,
      search,
      properties: Immutable.fromJS(properties),
      state: Immutable.fromJS(state),
      dashboardState,
      createdAt,
      owner,
    };
  }

  static create(): View {
    // eslint-disable-next-line no-use-before-define
    return new Builder().createdAt(new Date()).build();
  }

  get id(): string {
    return this._value.id;
  }

  get title(): string {
    return this._value.title;
  }

  get summary(): string {
    return this._value.summary;
  }

  get description(): string {
    return this._value.description;
  }

  get search(): Search {
    return this._value.search;
  }

  get properties(): Properties {
    return this._value.properties;
  }

  get state(): Immutable.Map<QueryId, ViewState> {
    return this._value.state;
  }

  get dashboardState(): DashboardState {
    return this._value.dashboardState;
  }

  get createdAt(): Date {
    return this._value.createdAt;
  }

  get widgetMapping(): WidgetMapping {
    return (this.state || Immutable.Map()).valueSeq().map(s => s.widgetMapping).reduce((prev, cur) => Immutable.fromJS(prev).merge(Immutable.fromJS(cur)));
  }

  get owner(): string {
    return this._value.owner;
  }

  toBuilder(): Builder {
    const { id, title, summary, description, search, properties, state, dashboardState, createdAt, owner } = this._value;
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
      owner,
    }));
  }

  toJSON() {
    const { id, title, summary, description, search, properties, state, dashboardState, createdAt, owner } = this._value;

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
      owner,
    };
  }

  static fromJSON(value: ViewJson): View {
    // eslint-disable-next-line camelcase
    const { id, title, summary, description, properties, state, dashboard_state, created_at, owner } = value;
    const viewState: Immutable.Map<QueryId, ViewState> = Immutable.Map(state).map(ViewState.fromJSON);
    const dashboardState = DashboardState.fromJSON(dashboard_state);
    return View.create()
      .toBuilder()
      .id(id)
      .title(title)
      .summary(summary)
      .description(description)
      .properties(properties)
      .state(viewState)
      .dashboardState(dashboardState)
      .createdAt(created_at)
      .owner(owner)
      .build();
  }

  static builder(): Builder {
    // eslint-disable-next-line no-use-before-define
    return new Builder();
  }
}

class Builder {
  value: Immutable.Map<string, any>;

  constructor(value: Immutable.Map = Immutable.Map()) {
    this.value = value;
  }

  id(value: string): Builder {
    return new Builder(this.value.set('id', value));
  }

  newId(): Builder {
    return this.id(ObjectID().toString());
  }

  title(value: string): Builder {
    return new Builder(this.value.set('title', value));
  }

  summary(value: string): Builder {
    return new Builder(this.value.set('summary', value));
  }

  description(value: string): Builder {
    return new Builder(this.value.set('description', value));
  }

  search(value: Search): Builder {
    return new Builder(this.value.set('search', value));
  }

  properties(value: Properties): Builder {
    return new Builder(this.value.set('properties', value));
  }

  state(value: ViewState): Builder {
    return new Builder(this.value.set('state', Immutable.fromJS(value)));
  }

  dashboardState(value: DashboardState): Builder {
    return new Builder(this.value.set('dashboardState', value));
  }

  createdAt(value: Date): Builder {
    return new Builder(this.value.set('createdAt', value));
  }

  owner(value: string): Builder {
    return new Builder(this.value.set('owner', value));
  }

  build(): View {
    const { id, title, summary, description, search, properties, state, dashboardState, createdAt, owner } = this.value.toObject();
    return new View(id, title, summary, description, search, properties, state, dashboardState, createdAt, owner);
  }
}

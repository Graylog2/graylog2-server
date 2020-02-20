// @flow strict
import * as Immutable from 'immutable';
import ObjectID from 'bson-objectid';

import ViewState from './ViewState';
import Search from '../search/Search';
import type { QueryId } from '../queries/Query';

export type Properties = Immutable.List<any>;

export type PluginMetadata = {
  name: string,
  url: string,
};
export type Requirements = { [string]: PluginMetadata };
export type ViewStateMap = Immutable.Map<QueryId, ViewState>;

export type SearchType = 'SEARCH';
export type DashboardType = 'DASHBOARD';
export type ViewType = SearchType | DashboardType;

type InternalState = {
  id: string,
  type: ViewType,
  title: string,
  summary: string,
  description: string,
  search: Search,
  properties: Properties,
  state: ViewStateMap,
  createdAt: Date,
  owner: string,
  requires: Requirements,
};

export type WidgetMapping = Immutable.Map<string, string>;
export type ViewJson = {
  id: string,
  type: ViewType,
  title: string,
  summary: string,
  description: string,
  search_id: string,
  properties: Properties,
  state: { [QueryId]: ViewState },
  created_at: Date,
  owner: string,
  requires: Requirements,
};

export default class View {
  static Type: { Search: SearchType, Dashboard: DashboardType } = {
    Search: 'SEARCH',
    Dashboard: 'DASHBOARD',
  };

  _value: InternalState;

  constructor(id: string,
    type: ViewType,
    title: string,
    summary: string,
    description: string,
    search: Search,
    properties: Properties,
    state: ViewStateMap,
    createdAt: Date,
    owner: string,
    requires: Requirements) {
    this._value = {
      id,
      type,
      title,
      summary,
      description,
      search,
      properties: Immutable.fromJS(properties),
      state: Immutable.fromJS(state),
      createdAt,
      owner,
      requires,
    };
  }

  static create(): View {
    // eslint-disable-next-line no-use-before-define
    return new Builder().createdAt(new Date()).build();
  }

  get id(): string {
    return this._value.id;
  }

  get type(): ViewType {
    return this._value.type;
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

  get state(): ViewStateMap {
    return this._value.state;
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

  get requires(): Requirements {
    return this._value.requires || {};
  }

  // eslint-disable-next-line no-use-before-define
  toBuilder(): Builder {
    const { id, title, summary, description, search, properties, state, createdAt, owner, requires, type } = this._value;
    // eslint-disable-next-line no-use-before-define
    return new Builder(Immutable.Map({
      id,
      title,
      summary,
      description,
      search,
      properties,
      state,
      createdAt,
      owner,
      requires,
      type,
    }));
  }

  toJSON() {
    const { id, type, title, summary, description, search, properties, state, createdAt, owner } = this._value;

    return {
      id,
      type,
      title,
      summary,
      description,
      search_id: search.id,
      properties,
      state,
      created_at: createdAt,
      owner,
    };
  }

  static fromJSON(value: ViewJson): View {
    // eslint-disable-next-line camelcase
    const { id, type, title, summary, description, properties, state, created_at, owner, requires } = value;
    const viewState: ViewStateMap = Immutable.Map(state).map(ViewState.fromJSON);
    return View.create()
      .toBuilder()
      .id(id)
      .type(type)
      .title(title)
      .summary(summary)
      .description(description)
      .properties(properties)
      .state(viewState)
      .createdAt(created_at)
      .owner(owner)
      .requires(requires)
      .build();
  }

  // eslint-disable-next-line no-use-before-define
  static builder(): Builder {
    // eslint-disable-next-line no-use-before-define
    return new Builder();
  }
}

type InternalBuilderState = Immutable.Map<string, any>
class Builder {
  value: InternalBuilderState;

  constructor(value: InternalBuilderState = Immutable.Map()) {
    this.value = value;
  }

  id(value: string): Builder {
    return new Builder(this.value.set('id', value));
  }

  toNewView(): Builder {
    return new Builder(this.value.set('id', undefined).set('title', undefined));
  }

  type(value: ViewType): Builder {
    return new Builder(this.value.set('type', value));
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

  state(value: ViewStateMap): Builder {
    return new Builder(this.value.set('state', Immutable.fromJS(value)));
  }

  createdAt(value: Date): Builder {
    return new Builder(this.value.set('createdAt', value));
  }

  owner(value: string): Builder {
    return new Builder(this.value.set('owner', value));
  }

  requires(value: Requirements): Builder {
    return new Builder(this.value.set('requires', value));
  }

  build(): View {
    const { id, type, title, summary, description, search, properties, state, createdAt, owner, requires } = this.value.toObject();
    return new View(id, type, title, summary, description, search, properties, state, createdAt, owner, requires);
  }
}

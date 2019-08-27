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

type InternalState = {
  id: string,
  title: string,
  summary: string,
  description: string,
  search: Search,
  properties: Properties,
  state: Immutable.Map<QueryId, ViewState>,
  createdAt: Date,
  owner: string,
  requires: Requirements,
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
  created_at: Date,
  owner: string,
  requires: Requirements,
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
    createdAt: Date,
    owner: string,
    requires: Requirements) {
    this._value = {
      id,
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
    return this._value.requires;
  }

  // eslint-disable-next-line no-use-before-define
  toBuilder(): Builder {
    const { id, title, summary, description, search, properties, state, createdAt, owner, requires } = this._value;
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
    }));
  }

  toJSON() {
    const { id, title, summary, description, search, properties, state, createdAt, owner } = this._value;

    return {
      id,
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
    const { id, title, summary, description, properties, state, created_at, owner, requires } = value;
    const viewState: Immutable.Map<QueryId, ViewState> = Immutable.Map(state).map(ViewState.fromJSON);
    return View.create()
      .toBuilder()
      .id(id)
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

  state(value: Immutable.Map<string, ViewState>): Builder {
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
    const { id, title, summary, description, search, properties, state, createdAt, owner, requires } = this.value.toObject();
    return new View(id, title, summary, description, search, properties, state, createdAt, owner, requires);
  }
}

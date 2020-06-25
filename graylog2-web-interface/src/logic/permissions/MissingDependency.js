// @flow strict
import * as Immutable from 'immutable';

import type { MissingDependency as MissingDependencyType } from 'logic/permissions/types';

type InternalState = MissingDependencyType;

export default class MissingDependency {
  _value: InternalState;

  constructor(
    id: $PropertyType<InternalState, 'id'>,
    owners: $PropertyType<InternalState, 'owners'>,
    title: $PropertyType<InternalState, 'title'>,
  ) {
    this._value = { id, owners, title };
  }

  get id(): $PropertyType<InternalState, 'id'> {
    return this._value.id;
  }

  get owners(): $PropertyType<InternalState, 'owners'> {
    return this._value.owners;
  }

  get title(): $PropertyType<InternalState, 'title'> {
    return this._value.title;
  }

  // eslint-disable-next-line no-use-before-define
  toBuilder(): Builder {
    const { id, owners, title } = this._value;
    // eslint-disable-next-line no-use-before-define
    return new Builder(Immutable.Map({ id, owners, title }));
  }

  toJSON() {
    const { id, owners, title } = this._value;
    return { id, owners, title };
  }

  static fromJSON(value: MissingDependencyType): MissingDependency {
    const { id, owners, title } = value;
    return MissingDependency
      .builder()
      .id(id)
      .owners(owners)
      .title(title)
      .build();
  }

  // eslint-disable-next-line no-use-before-define
  static builder(): Builder {
    // eslint-disable-next-line no-use-before-define
    return new Builder();
  }
}

type InternalBuilderState = Immutable.Map<string, any>;

class Builder {
  value: InternalBuilderState;

  constructor(value: InternalBuilderState = Immutable.Map()) {
    this.value = value;
  }

  id(value: $PropertyType<InternalState, 'id'>): Builder {
    return new Builder(this.value.set('id', value));
  }

  owners(value: $PropertyType<InternalState, 'owners'>): Builder {
    return new Builder(this.value.set('owners', value));
  }

  title(value: $PropertyType<InternalState, 'title'>): Builder {
    return new Builder(this.value.set('title', value));
  }

  build(): MissingDependency {
    const { id, owners, title } = this.value.toObject();
    return new MissingDependency(id, owners, title);
  }
}

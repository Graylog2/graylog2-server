// @flow strict
import * as Immutable from 'immutable';

import type { Role as RoleType } from 'logic/permissions/types';

type InternalState = RoleType;

export default class Role {
  _value: InternalState;

  constructor(
    id: $PropertyType<InternalState, 'id'>,
    title: $PropertyType<InternalState, 'title'>,
  ) {
    this._value = { id, title };
  }

  get id(): $PropertyType<InternalState, 'id'> {
    return this._value.id;
  }

  get title(): $PropertyType<InternalState, 'title'> {
    return this._value.title;
  }

  // eslint-disable-next-line no-use-before-define
  toBuilder(): Builder {
    const { id, title } = this._value;

    // eslint-disable-next-line no-use-before-define
    return new Builder(Immutable.Map({ id, title }));
  }

  toJSON() {
    const { id, title } = this._value;

    return { id, title };
  }

  static fromJSON(value: InternalState): Role {
    const { id, title } = value;

    return Role
      .builder()
      .id(id)
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

  title(value: $PropertyType<InternalState, 'title'>): Builder {
    return new Builder(this.value.set('title', value));
  }

  build(): Role {
    const { id, title } = this.value.toObject();

    return new Role(id, title);
  }
}

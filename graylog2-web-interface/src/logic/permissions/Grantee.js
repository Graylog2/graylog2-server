// @flow strict
import * as Immutable from 'immutable';

import type { Grantee as GranteeType } from 'logic/permissions/types';

type InternalState = GranteeType;

export default class Grantee {
  _value: InternalState;

  constructor(
    id: $PropertyType<InternalState, 'id'>,
    title: $PropertyType<InternalState, 'title'>,
    type: $PropertyType<InternalState, 'type'>,
  ) {
    this._value = { id, title, type };
  }

  get id(): $PropertyType<InternalState, 'id'> {
    return this._value.id;
  }

  get title(): $PropertyType<InternalState, 'title'> {
    return this._value.title;
  }

  get type(): $PropertyType<InternalState, 'type'> {
    return this._value.type;
  }

  // eslint-disable-next-line no-use-before-define
  toBuilder(): Builder {
    const { id, title, type } = this._value;

    // eslint-disable-next-line no-use-before-define
    return new Builder(Immutable.Map({ id, title, type }));
  }

  toJSON() {
    const { id, title, type } = this._value;

    return { id, title, type };
  }

  static fromJSON(value: GranteeType): Grantee {
    const { id, title, type } = value;

    return Grantee
      .builder()
      .id(id)
      .title(title)
      .type(type)
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

  type(value: $PropertyType<InternalState, 'type'>): Builder {
    return new Builder(this.value.set('type', value));
  }

  build(): Grantee {
    const { id, title, type } = this.value.toObject();

    return new Grantee(id, title, type);
  }
}

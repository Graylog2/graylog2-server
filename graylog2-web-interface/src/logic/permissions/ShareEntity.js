// @flow strict
import * as Immutable from 'immutable';

import type { ShareEntity as ShareEntityType } from 'logic/permissions/types';

import Grantee from './Grantee';

type InternalState = {
  id: $PropertyType<ShareEntityType, 'id'>,
  owners: Immutable.List<Grantee>,
  title: $PropertyType<ShareEntityType, 'title'>,
  type: $PropertyType<ShareEntityType, 'type'>,
};

export default class ShareEntity {
  _value: InternalState;

  constructor(
    id: $PropertyType<InternalState, 'id'>,
    owners: $PropertyType<InternalState, 'owners'>,
    title: $PropertyType<InternalState, 'title'>,
    type: $PropertyType<InternalState, 'type'>,
  ) {
    this._value = { id, owners, title, type };
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

  get type(): $PropertyType<InternalState, 'type'> {
    return this._value.type;
  }

  // eslint-disable-next-line no-use-before-define
  toBuilder(): Builder {
    const { id, owners, title } = this._value;

    // eslint-disable-next-line no-use-before-define
    return new Builder(Immutable.Map({ id, owners, title }));
  }

  toJSON() {
    const { id, owners, title, type } = this._value;

    return { id, owners, title, type };
  }

  static fromJSON(value: ShareEntityType): ShareEntity {
    const { id, owners, title, type } = value;
    const formattedOwners = Immutable.fromJS(owners.map((o) => Grantee.fromJSON(o)));

    return ShareEntity
      .builder()
      .id(id)
      .owners(formattedOwners)
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

  owners(value: $PropertyType<InternalState, 'owners'>): Builder {
    return new Builder(this.value.set('owners', value));
  }

  title(value: $PropertyType<InternalState, 'title'>): Builder {
    return new Builder(this.value.set('title', value));
  }

  type(value: $PropertyType<InternalState, 'type'>): Builder {
    return new Builder(this.value.set('type', value));
  }

  build(): ShareEntity {
    const { id, owners, title, type } = this.value.toObject();

    return new ShareEntity(id, owners, title, type);
  }
}

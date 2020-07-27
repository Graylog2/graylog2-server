// @flow strict
import * as Immutable from 'immutable';

import type { DescriptiveItem } from 'components/users/PaginatedItemOverview';

type InternalState = {
  id: string,
  name: string,
  description: string,
  permissions: Immutable.Set<string>,
  readOnly: boolean,
};

export type RoleJSON = {
  id: string,
  name: string,
  description: string,
  permissions: Immutable.Set<string>,
  read_only: boolean,
};

export default class Role {
  _value: InternalState;

  // eslint-disable-next-line no-undef
  constructor(
    id: $PropertyType<InternalState, 'id'>,
    name: $PropertyType<InternalState, 'name'>,
    description: $PropertyType<InternalState, 'description'>,
    permissions: $PropertyType<InternalState, 'permissions'>,
    readOnly: $PropertyType<InternalState, 'readOnly'>,
  ) {
    this._value = {
      id,
      name,
      description,
      permissions,
      readOnly,
    };
  }

  get id() {
    return this._value.id;
  }

  get name() {
    return this._value.name;
  }

  get description() {
    return this._value.description;
  }

  get permissions() {
    return this._value.permissions;
  }

  get readOnly() {
    return this._value.readOnly;
  }

  toBuilder() {
    const {
      id,
      name,
      description,
      permissions,
      readOnly,
    } = this._value;

    // eslint-disable-next-line no-use-before-define
    return new Builder(Immutable.Map({
      id,
      name,
      description,
      permissions,
      readOnly,
    }));
  }

  // eslint-disable-next-line no-undef
  static create(
    id: $PropertyType<InternalState, 'id'>,
    name: $PropertyType<InternalState, 'name'>,
    description: $PropertyType<InternalState, 'description'>,
    permissions: $PropertyType<InternalState, 'permissions'>,
    readOnly: $PropertyType<InternalState, 'readOnly'>,
  ) {
    return new Role(
      id,
      name,
      description,
      permissions,
      readOnly,
    );
  }

  toJSON() {
    const {
      id,
      name,
      description,
      permissions,
      readOnly,
    } = this._value;

    return {
      id,
      name,
      description,
      permissions,
      read_only: readOnly,
    };
  }

  static fromJSON(value: RoleJSON) {
    const {
      id,
      name,
      description,
      permissions,
      read_only: readOnly,
    } = value;

    return Role.create(
      id,
      name,
      description,
      permissions,
      readOnly,
    );
  }
}

type BuilderState = Immutable.Map<string, any>;

class Builder {
  value: BuilderState;

  constructor(value: BuilderState = Immutable.Map()) {
    this.value = value;
  }

  id(value: $PropertyType<InternalState, 'id'>) {
    return new Builder(this.value.set('id', value));
  }

  name(value: $PropertyType<InternalState, 'name'>) {
    return new Builder(this.value.set('name', value));
  }

  description(value: $PropertyType<InternalState, 'description'>) {
    return new Builder(this.value.set('description', value));
  }

  permissions(value: $PropertyType<InternalState, 'permissions'>) {
    return new Builder(this.value.set('permissions', value));
  }

  readOnly(value: $PropertyType<InternalState, 'readOnly'>) {
    return new Builder(this.value.set('readOnly', value));
  }

  build() {
    const {
      id,
      name,
      description,
      permissions,
      readOnly,
    } = this.value.toObject();

    return new Role(
      id,
      name,
      description,
      permissions,
      readOnly,
    );
  }
}

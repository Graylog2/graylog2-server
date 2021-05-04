/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import * as Immutable from 'immutable';
import { $PropertyType } from 'utility-types';

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

    // eslint-disable-next-line @typescript-eslint/no-use-before-define
    return new Builder(Immutable.Map({
      id,
      name,
      description,
      permissions,
      readOnly,
    }));
  }

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

  static builder(): Builder {
    // eslint-disable-next-line @typescript-eslint/no-use-before-define
    return new Builder();
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

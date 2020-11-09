// @flow strict
import * as Immutable from 'immutable';

export type UserContext = {
  id: string,
  username: string,
};

type InternalState = {
  id: string,
  name: string,
  description: string,
  permissions: Immutable.Set<string>,
  readOnly: boolean,
  users: Immutable.Set<UserContext>,
};

export type RoleContext = {
  users: { [string]: UserContext[] },
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
    users: $PropertyType<InternalState, 'users'>,
  ) {
    this._value = {
      id,
      name,
      description,
      permissions,
      readOnly,
      users,
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

  get users() {
    return this._value.users;
  }

  toBuilder() {
    const {
      id,
      name,
      description,
      permissions,
      readOnly,
      users,
    } = this._value;

    // eslint-disable-next-line no-use-before-define
    return new Builder(Immutable.Map({
      id,
      name,
      description,
      permissions,
      readOnly,
      users,
    }));
  }

  // eslint-disable-next-line no-undef
  static create(
    id: $PropertyType<InternalState, 'id'>,
    name: $PropertyType<InternalState, 'name'>,
    description: $PropertyType<InternalState, 'description'>,
    permissions: $PropertyType<InternalState, 'permissions'>,
    readOnly: $PropertyType<InternalState, 'readOnly'>,
    users: $PropertyType<InternalState, 'users'>,
  ) {
    return new Role(
      id,
      name,
      description,
      permissions,
      readOnly,
      users,
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

  static fromJSON(value: RoleJSON, userContext: UserContext[]) {
    const users = Immutable.Set(userContext || []);

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
      users,
    );
  }

  // eslint-disable-next-line no-use-before-define
  static builder(): Builder {
    // eslint-disable-next-line no-use-before-define
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

  users(value: $PropertyType<InternalState, 'users'>) {
    return new Builder(this.value.set('users', value));
  }

  build() {
    const {
      id,
      name,
      description,
      permissions,
      readOnly,
      users,
    } = this.value.toObject();

    return new Role(
      id,
      name,
      description,
      permissions,
      readOnly,
      users,
    );
  }
}

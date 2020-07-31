// @flow strict
import * as Immutable from 'immutable';

import type { UserJSON } from 'stores/users/UsersStore';

type InternalState = {
  id: string,
  username: string,
  fullName: string,
  email: string,
  roles: Immutable.List<string>,
  readOnly: boolean,
  external: boolean,
  sessionActive: boolean,
  clientAddress: string,
  lastActivity: string,
};

export default class UserOverview {
  _value: InternalState;

  constructor(
    id: $PropertyType<InternalState, 'id'>,
    username: $PropertyType<InternalState, 'username'>,
    fullName: $PropertyType<InternalState, 'fullName'>,
    email: $PropertyType<InternalState, 'email'>,
    roles: $PropertyType<InternalState, 'roles'>,
    readOnly: $PropertyType<InternalState, 'readOnly'>,
    external: $PropertyType<InternalState, 'external'>,
    sessionActive: $PropertyType<InternalState, 'sessionActive'>,
    clientAddress: $PropertyType<InternalState, 'clientAddress'>,
    lastActivity: $PropertyType<InternalState, 'lastActivity'>,
  ) {
    this._value = {
      id,
      username,
      fullName,
      email,
      roles,
      readOnly,
      external,
      sessionActive,
      clientAddress,
      lastActivity,
    };
  }

  get id() {
    return this._value.id;
  }

  get username() {
    return this._value.username;
  }

  get name() {
    return this._value.username;
  }

  get fullName() {
    return this._value.fullName;
  }

  get description() {
    return this._value.fullName;
  }

  get email() {
    return this._value.email;
  }

  get roles() {
    return this._value.roles;
  }

  get readOnly() {
    return this._value.readOnly;
  }

  get external() {
    return this._value.external;
  }

  get sessionActive() {
    return this._value.sessionActive;
  }

  get clientAddress() {
    return this._value.clientAddress;
  }

  get lastActivity() {
    return this._value.lastActivity;
  }

  toBuilder() {
    const {
      id,
      username,
      fullName,
      email,
      roles,
      readOnly,
      external,
      sessionActive,
      clientAddress,
      lastActivity,
    } = this._value;

    // eslint-disable-next-line no-use-before-define
    return new Builder(Immutable.Map({
      id,
      username,
      fullName,
      email,
      roles,
      readOnly,
      external,
      sessionActive,
      clientAddress,
      lastActivity,
    }));
  }

  static create(
    id: $PropertyType<InternalState, 'id'>,
    username: $PropertyType<InternalState, 'username'>,
    fullName: $PropertyType<InternalState, 'fullName'>,
    email: $PropertyType<InternalState, 'email'>,
    roles: $PropertyType<InternalState, 'roles'>,
    readOnly: $PropertyType<InternalState, 'readOnly'>,
    external: $PropertyType<InternalState, 'external'>,
    sessionActive: $PropertyType<InternalState, 'sessionActive'>,
    clientAddress: $PropertyType<InternalState, 'clientAddress'>,
    lastActivity: $PropertyType<InternalState, 'lastActivity'>,
  ) {
    return new UserOverview(
      id,
      username,
      fullName,
      email,
      roles,
      readOnly,
      external,
      sessionActive,
      clientAddress,
      lastActivity,
    );
  }

  toJSON(): UserJSON {
    const {
      id,
      username,
      fullName,
      email,
      roles,
      readOnly,
      external,
      sessionActive,
      clientAddress,
      lastActivity,
    } = this._value;

    return {
      id,
      username,
      full_name: fullName,
      email,
      permissions: [],
      timezone: '',
      preferences: undefined,
      roles: roles.toArray(),
      read_only: readOnly,
      external,
      session_timeout_ms: 0,
      startpage: undefined,
      session_active: sessionActive,
      client_address: clientAddress,
      last_activity: lastActivity,
    };
  }

  static fromJSON(value: UserJSON) {
    const {
      id,
      username,
      full_name: fullName,
      email,
      roles,
      read_only: readOnly,
      external,
      session_active: sessionActive,
      client_address: clientAddress,
      last_activity: lastActivity,
    } = value;

    return UserOverview.create(
      id,
      username,
      fullName,
      email,
      Immutable.List(roles),
      readOnly,
      external,
      sessionActive,
      clientAddress,
      lastActivity,
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

  username(value: $PropertyType<InternalState, 'username'>) {
    return new Builder(this.value.set('username', value));
  }

  fullName(value: $PropertyType<InternalState, 'fullName'>) {
    return new Builder(this.value.set('fullName', value));
  }

  email(value: $PropertyType<InternalState, 'email'>) {
    return new Builder(this.value.set('email', value));
  }

  roles(value: $PropertyType<InternalState, 'roles'>) {
    return new Builder(this.value.set('roles', value));
  }

  readOnly(value: $PropertyType<InternalState, 'readOnly'>) {
    return new Builder(this.value.set('readOnly', value));
  }

  external(value: $PropertyType<InternalState, 'external'>) {
    return new Builder(this.value.set('external', value));
  }

  sessionActive(value: $PropertyType<InternalState, 'sessionActive'>) {
    return new Builder(this.value.set('sessionActive', value));
  }

  clientAddress(value: $PropertyType<InternalState, 'clientAddress'>) {
    return new Builder(this.value.set('clientAddress', value));
  }

  lastActivity(value: $PropertyType<InternalState, 'lastActivity'>) {
    return new Builder(this.value.set('lastActivity', value));
  }

  build() {
    const {
      id,
      username,
      fullName,
      email,
      roles,
      readOnly,
      external,
      sessionActive,
      clientAddress,
      lastActivity,
    } = this.value.toObject();

    return new UserOverview(
      id,
      username,
      fullName,
      email,
      roles,
      readOnly,
      external,
      sessionActive,
      clientAddress,
      lastActivity,
    );
  }
}

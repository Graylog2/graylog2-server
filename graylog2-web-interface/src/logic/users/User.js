// @flow strict
import * as Immutable from 'immutable';

import type { User as UserJSON } from 'stores/users/UsersStore.js';

type StartPage = {
  id: string,
  type: string,
};

type InternalState = {
  id: string,
  username: string,
  fullName: string,
  email: string,
  permissions: Immutable.List<string>,
  timezone: string,
  preferences?: any,
  roles: Immutable.List<string>,
  readOnly: boolean,
  external: boolean,
  sessionTimeoutMs: number,
  startpage?: StartPage,
};

export default class User {
  _value: InternalState;

  // eslint-disable-next-line no-undef
  constructor(
    id: $PropertyType<InternalState, 'id'>,
    username: $PropertyType<InternalState, 'username'>,
    fullName: $PropertyType<InternalState, 'fullName'>,
    email: $PropertyType<InternalState, 'email'>,
    permissions: $PropertyType<InternalState, 'permissions'>,
    timezone: $PropertyType<InternalState, 'timezone'>,
    preferences: $PropertyType<InternalState, 'preferences'>,
    roles: $PropertyType<InternalState, 'roles'>,
    readOnly: $PropertyType<InternalState, 'readOnly'>,
    external: $PropertyType<InternalState, 'external'>,
    sessionTimeoutMs: $PropertyType<InternalState, 'sessionTimeoutMs'>,
    startpage: $PropertyType<InternalState, 'startpage'>,
  ) {
    this._value = {
      id,
      username,
      fullName,
      email,
      permissions,
      timezone,
      preferences,
      roles,
      readOnly,
      external,
      sessionTimeoutMs,
      startpage,
    };
  }

  get id() {
    return this._value.id;
  }

  get username() {
    return this._value.username;
  }

  get fullName() {
    return this._value.fullName;
  }

  get email() {
    return this._value.email;
  }

  get permissions() {
    return this._value.permissions;
  }

  get timezone() {
    return this._value.timezone;
  }

  get preferences() {
    return this._value.preferences;
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

  get sessionTimeoutMs() {
    return this._value.sessionTimeoutMs;
  }

  get startpage() {
    return this._value.startpage;
  }

  toBuilder() {
    const {
      id,
      username,
      fullName,
      email,
      permissions,
      timezone,
      preferences,
      roles,
      readOnly,
      external,
      sessionTimeoutMs,
      startpage,
    } = this._value;

    // eslint-disable-next-line no-use-before-define
    return new Builder(Immutable.Map({
      id,
      username,
      fullName,
      email,
      permissions,
      timezone,
      preferences,
      roles,
      readOnly,
      external,
      sessionTimeoutMs,
      startpage,
    }));
  }

  // eslint-disable-next-line no-undef
  static create(
    id: $PropertyType<InternalState, 'id'>,
    username: $PropertyType<InternalState, 'username'>,
    fullName: $PropertyType<InternalState, 'fullName'>,
    email: $PropertyType<InternalState, 'email'>,
    permissions: $PropertyType<InternalState, 'permissions'>,
    timezone: $PropertyType<InternalState, 'timezone'>,
    preferences: $PropertyType<InternalState, 'preferences'>,
    roles: $PropertyType<InternalState, 'roles'>,
    readOnly: $PropertyType<InternalState, 'readOnly'>,
    external: $PropertyType<InternalState, 'external'>,
    sessionTimeoutMs: $PropertyType<InternalState, 'sessionTimeoutMs'>,
    startpage: $PropertyType<InternalState, 'startpage'>,
  ) {
    return new User(
      id,
      username,
      fullName,
      email,
      permissions,
      timezone,
      preferences,
      roles,
      readOnly,
      external,
      sessionTimeoutMs,
      startpage,
    );
  }

  toJSON(): UserJSON {
    const {
      id,
      username,
      fullName,
      email,
      permissions,
      timezone,
      preferences,
      roles,
      readOnly,
      external,
      sessionTimeoutMs,
      startpage,
    } = this._value;

    return {
      id,
      username,
      full_name: fullName,
      email,
      permissions: permissions.toArray(),
      timezone,
      preferences,
      roles: roles.toArray(),
      read_only: readOnly,
      external,
      session_timeout_ms: sessionTimeoutMs,
      startpage,
    };
  }

  static fromJSON(value: UserJSON) {
    const {
      id,
      username,
      // eslint-disable-next-line camelcase
      full_name,
      email,
      permissions,
      timezone,
      preferences,
      roles,
      // eslint-disable-next-line camelcase
      read_only,
      external,
      // eslint-disable-next-line camelcase
      session_timeout_ms,
      startpage,
    } = value;

    return User.create(
      id,
      username,
      full_name,
      email,
      Immutable.List(permissions),
      timezone,
      preferences,
      Immutable.List(roles),
      read_only,
      external,
      session_timeout_ms,
      startpage,
    );
  }
}

type BuilderState = Immutable.Map<string, any>;

class Builder {
  value: BuilderState;

  constructor(value: BuilderState = Immutable.Map()) {
    this.value = value;
  }

  // eslint-disable-next-line no-undef
  id(value: $PropertyType<InternalState, 'id'>) {
    return new Builder(this.value.set('id', value));
  }

  // eslint-disable-next-line no-undef
  username(value: $PropertyType<InternalState, 'username'>) {
    return new Builder(this.value.set('username', value));
  }

  // eslint-disable-next-line no-undef
  fullName(value: $PropertyType<InternalState, 'fullName'>) {
    return new Builder(this.value.set('fullName', value));
  }

  // eslint-disable-next-line no-undef
  email(value: $PropertyType<InternalState, 'email'>) {
    return new Builder(this.value.set('email', value));
  }

  // eslint-disable-next-line no-undef
  permissions(value: $PropertyType<InternalState, 'permissions'>) {
    return new Builder(this.value.set('permissions', value));
  }

  // eslint-disable-next-line no-undef
  timezone(value: $PropertyType<InternalState, 'timezone'>) {
    return new Builder(this.value.set('timezone', value));
  }

  // eslint-disable-next-line no-undef
  preferences(value: $PropertyType<InternalState, 'preferences'>) {
    return new Builder(this.value.set('preferences', value));
  }

  // eslint-disable-next-line no-undef
  roles(value: $PropertyType<InternalState, 'roles'>) {
    return new Builder(this.value.set('roles', value));
  }

  // eslint-disable-next-line no-undef
  external(value: $PropertyType<InternalState, 'external'>) {
    return new Builder(this.value.set('external', value));
  }

  // eslint-disable-next-line no-undef
  sessionTimeoutMs(value: $PropertyType<InternalState, 'sessionTimeoutMs'>) {
    return new Builder(this.value.set('sessionTimoutMs', value));
  }

  // eslint-disable-next-line no-undef
  startpage(value: $PropertyType<InternalState, 'startpage'>) {
    return new Builder(this.value.set('startpage', value));
  }

  build() {
    const {
      id,
      username,
      fullName,
      email,
      permissions,
      timezone,
      preferences,
      roles,
      readOnly,
      external,
      sessionTimeoutMs,
      startpage,
    } = this.value.toObject();

    return new User(
      id,
      username,
      fullName,
      email,
      permissions,
      timezone,
      preferences,
      roles,
      readOnly,
      external,
      sessionTimeoutMs,
      startpage,
    );
  }
}

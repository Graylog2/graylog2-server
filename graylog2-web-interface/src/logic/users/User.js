// @flow strict
import * as Immutable from 'immutable';

import type { UserJSON } from 'stores/users/UsersStore';

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
  sessionActive: boolean,
  clientAddress: string,
  lastActivity: string,
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
    sessionActive: $PropertyType<InternalState, 'sessionActive'>,
    clientAddress: $PropertyType<InternalState, 'clientAddress'>,
    lastActivity: $PropertyType<InternalState, 'lastActivity'>,
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

  get sessionTimeout() {
    if (!this.sessionTimeoutMs) {
      return undefined;
    }

    const MS_DAY = 24 * 60 * 60 * 1000;
    const MS_HOUR = 60 * 60 * 1000;
    const MS_MINUTE = 60 * 1000;
    const MS_SECOND = 1000;

    const _estimateUnit = (value) => {
      if (value === 0) {
        return [MS_SECOND, 'Seconds'];
      }

      if (value % MS_DAY === 0) {
        return [MS_DAY, 'Days'];
      }

      if (value % MS_HOUR === 0) {
        return [MS_HOUR, 'Hours'];
      }

      if (value % MS_MINUTE === 0) {
        return [MS_MINUTE, 'Minutes'];
      }

      return [MS_SECOND, 'Seconds'];
    };

    const unit = _estimateUnit(this.sessionTimeoutMs);
    const value = Math.floor(this.sessionTimeoutMs / unit[0]);

    return {
      value,
      unitMS: unit[0],
      unitString: unit[1],
    };
  }

  get startpage() {
    return this._value.startpage;
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
      permissions,
      timezone,
      preferences,
      roles,
      readOnly,
      external,
      sessionTimeoutMs,
      startpage,
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
      permissions,
      timezone,
      preferences,
      roles,
      readOnly,
      external,
      sessionTimeoutMs,
      startpage,
      sessionActive,
      clientAddress,
      lastActivity,
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
    sessionActive: $PropertyType<InternalState, 'sessionActive'>,
    clientAddress: $PropertyType<InternalState, 'clientAddress'>,
    lastActivity: $PropertyType<InternalState, 'lastActivity'>,
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
      permissions,
      timezone,
      preferences,
      roles,
      readOnly,
      external,
      sessionTimeoutMs,
      startpage,
      sessionActive,
      clientAddress,
      lastActivity,
    } = this._value;

    return {
      id,
      username,
      full_name: fullName,
      email,
      permissions: permissions ? permissions.toArray() : [],
      timezone,
      preferences,
      roles: roles ? roles.toArray() : [],
      read_only: readOnly,
      external,
      session_timeout_ms: sessionTimeoutMs,
      startpage,
      session_active: sessionActive,
      client_address: clientAddress,
      last_activity: lastActivity,
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
      // eslint-disable-next-line camelcase
      session_active,
      // eslint-disable-next-line camelcase
      client_address,
      // eslint-disable-next-line camelcase
      last_activity,
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
      session_active,
      client_address,
      last_activity,
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
  readOnly(value: $PropertyType<InternalState, 'readOnly'>) {
    return new Builder(this.value.set('readOnly', value));
  }

  // eslint-disable-next-line no-undef
  external(value: $PropertyType<InternalState, 'external'>) {
    return new Builder(this.value.set('external', value));
  }

  // eslint-disable-next-line no-undef
  sessionTimeoutMs(value: $PropertyType<InternalState, 'sessionTimeoutMs'>) {
    return new Builder(this.value.set('sessionTimeoutMs', value));
  }

  // eslint-disable-next-line no-undef
  startpage(value: $PropertyType<InternalState, 'startpage'>) {
    return new Builder(this.value.set('startpage', value));
  }

  // eslint-disable-next-line no-undef
  sessionActive(value: $PropertyType<InternalState, 'sessionActive'>) {
    return new Builder(this.value.set('sessionActive', value));
  }

  // eslint-disable-next-line no-undef
  clientAddress(value: $PropertyType<InternalState, 'clientAddress'>) {
    return new Builder(this.value.set('clientAddress', value));
  }

  // eslint-disable-next-line no-undef
  lastActivity(value: $PropertyType<InternalState, 'lastActivity'>) {
    return new Builder(this.value.set('lastActivity', value));
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
      sessionActive,
      clientAddress,
      lastActivity,
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
      sessionActive,
      clientAddress,
      lastActivity,
    );
  }
}

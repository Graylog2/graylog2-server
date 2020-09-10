// @flow strict
import * as Immutable from 'immutable';

export type AuthenticationUserJSON = {
  id: string,
  username: string,
  full_name: string,
  email: string,
  external_user: ?boolean,
  roles: Array<string>,
  read_only: ?boolean,
  session_active: ?boolean,
  client_address: string,
  last_activity: ?string,
  backend_id: string,
  backend_guid: string,
  enabled: boolean,
};

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
  lastActivity: ?string,
  backendId: string,
  backendGuid: string,
  enabled: boolean,
};

export default class AuthenticationUser {
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
    backendId: $PropertyType<InternalState, 'backendId'>,
    backendGuid: $PropertyType<InternalState, 'backendGuid'>,
    enabled: $PropertyType<InternalState, 'enabled'>,
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
      backendId,
      backendGuid,
      enabled,
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

  get backendId() {
    return this._value.backendId;
  }

  get backendGuid() {
    return this._value.backendGuid;
  }

  get enabled() {
    return this._value.enabled;
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
      backendId,
      backendGuid,
      enabled,
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
      backendId,
      backendGuid,
      enabled,
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
    backendId: $PropertyType<InternalState, 'backendId'>,
    backendGuid: $PropertyType<InternalState, 'backendGuid'>,
    enabled: $PropertyType<InternalState, 'enabled'>,
  ) {
    return new AuthenticationUser(
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
      backendId,
      backendGuid,
      enabled,
    );
  }

  toJSON(): AuthenticationUserJSON {
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
      backendId,
      backendGuid,
      enabled,
    } = this._value;

    return {
      id,
      username,
      full_name: fullName,
      email,
      roles: roles.toArray(),
      read_only: readOnly,
      external_user: external,
      session_active: sessionActive,
      client_address: clientAddress,
      last_activity: lastActivity,
      backedn_id: backendId,
      backedn_guid: backendGuid,
      enabled,
    };
  }

  static fromJSON(value: AuthenticationUserJSON) {
    const {
      id,
      username,
      full_name: fullName,
      email,
      roles,
      read_only: readOnly,
      external_user: external,
      session_active: sessionActive,
      client_address: clientAddress,
      last_activity: lastActivity,
      backend_id: backendId,
      backend_guid: backendGuid,
      enabled,
    } = value;

    return AuthenticationUser.create(
      id,
      username,
      fullName,
      email,
      Immutable.List(roles),
      readOnly ?? false,
      external ?? false,
      sessionActive ?? false,
      clientAddress,
      lastActivity,
      backendId,
      backendGuid,
      enabled,
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

  backendId(value: $PropertyType<InternalState, 'backendId'>) {
    return new Builder(this.value.set('backendId', value));
  }

  backendGuid(value: $PropertyType<InternalState, 'backendGuid'>) {
    return new Builder(this.value.set('backendGuid', value));
  }

  enabled(value: $PropertyType<InternalState, 'enabled'>) {
    return new Builder(this.value.set('enabled', value));
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
      backendId,
      backendGuid,
      enabled,
    } = this.value.toObject();

    return new AuthenticationUser(
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
      backendId,
      backendGuid,
      enabled,
    );
  }
}

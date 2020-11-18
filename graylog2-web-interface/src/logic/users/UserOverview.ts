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
import { $PropertyType } from 'utility-types';
import * as Immutable from 'immutable';

export type AccountStatus = 'enabled' | 'disabled' | 'deleted';

/* eslint-disable camelcase */
export type UserOverviewJSON = {
  id: string;
  username: string;
  full_name: string;
  email: string;
  external_user: boolean | null | undefined;
  roles: Array<string>;
  read_only: boolean | null | undefined;
  session_active: boolean | null | undefined;
  client_address: string;
  last_activity: string | null | undefined;
  enabled: boolean;
  auth_service_id: string;
  auth_service_uid: string;
  account_status: AccountStatus;
};
/* eslint-enable camelcase */

type InternalState = {
  id: string;
  username: string;
  fullName: string;
  email: string;
  roles: Immutable.Set<string>;
  readOnly: boolean;
  external: boolean;
  sessionActive: boolean;
  clientAddress: string;
  lastActivity: string | null | undefined;
  enabled: boolean;
  authServiceId: string;
  authServiceUid: string;
  accountStatus: AccountStatus;
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
    enabled: $PropertyType<InternalState, 'enabled'>,
    authServiceId: $PropertyType<InternalState, 'authServiceId'>,
    authServiceUid: $PropertyType<InternalState, 'authServiceUid'>,
    accountStatus: $PropertyType<InternalState, 'accountStatus'>,
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
      enabled,
      authServiceId,
      authServiceUid,
      accountStatus,
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

  get enabled() {
    return this._value.enabled;
  }

  get authServiceId() {
    return this._value.authServiceId;
  }

  get authServiceUid() {
    return this._value.authServiceUid;
  }

  get accountStatus() {
    return this._value.accountStatus;
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
      enabled,
      authServiceId,
      authServiceUid,
      accountStatus,
    } = this._value;

    // eslint-disable-next-line no-use-before-define,@typescript-eslint/no-use-before-define
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
      enabled,
      authServiceId,
      authServiceUid,
      accountStatus,
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
    enabled: $PropertyType<InternalState, 'enabled'>,
    authServiceId: $PropertyType<InternalState, 'authServiceId'>,
    authServiceUid: $PropertyType<InternalState, 'authServiceUid'>,
    accountStatus: $PropertyType<InternalState, 'accountStatus'>,
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
      enabled,
      authServiceId,
      authServiceUid,
      accountStatus,
    );
  }

  toJSON(): UserOverviewJSON {
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
      enabled,
      authServiceId,
      authServiceUid,
      accountStatus,
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
      enabled,
      auth_service_id: authServiceId,
      auth_service_uid: authServiceUid,
      account_status: accountStatus,
    };
  }

  static fromJSON(value: UserOverviewJSON) {
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
      enabled,
      auth_service_id: authServiceId,
      auth_service_uid: authServiceUid,
      account_status: accountStatus,
    } = value;

    return UserOverview.create(id,
      username,
      fullName,
      email,
      Immutable.Set(roles),
      readOnly ?? false,
      external ?? false,
      sessionActive ?? false,
      clientAddress,
      lastActivity,
      enabled,
      authServiceId,
      authServiceUid,
      accountStatus);
  }

  // eslint-disable-next-line no-use-before-define
  static builder(): Builder {
    // eslint-disable-next-line no-use-before-define,@typescript-eslint/no-use-before-define
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

  enabled(value: $PropertyType<InternalState, 'enabled'>) {
    return new Builder(this.value.set('enabled', value));
  }

  authServiceId(value: $PropertyType<InternalState, 'authServiceId'>) {
    return new Builder(this.value.set('authServiceId', value));
  }

  authServiceUid(value: $PropertyType<InternalState, 'authServiceUid'>) {
    return new Builder(this.value.set('authServiceUid', value));
  }

  accountStatus(value: $PropertyType<InternalState, 'accountStatus'>) {
    return new Builder(this.value.set('accountStatus', value));
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
      enabled,
      authServiceId,
      authServiceUid,
      accountStatus,
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
      enabled,
      authServiceId,
      authServiceUid,
      accountStatus,
    );
  }
}

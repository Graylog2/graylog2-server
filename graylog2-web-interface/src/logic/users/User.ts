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

import type { PreferencesMap } from 'stores/users/PreferencesStore';

import type { AccountStatus } from './UserOverview';

export type StartPage = {
  id?: string;
  type: 'dashboard' | 'stream' | 'search' | 'graylog_security_welcome';
};

export type UserJSON = {
  client_address: string;
  email: string;
  external: boolean;
  full_name: string;
  first_name: string;
  last_name: string;
  id: string;
  last_activity: string | null | undefined;
  permissions: string[];
  grn_permissions?: string[];
  preferences: PreferencesMap;
  read_only: boolean;
  roles: string[];
  session_active: boolean;
  session_timeout_ms: number;
  startpage?: StartPage;
  timezone: string | null | undefined;
  username: string;
  account_status: AccountStatus;
  service_account: boolean;
  auth_service_enabled: boolean;
};

type InternalState = {
  id: string;
  username: string;
  fullName: string;
  firstName: string;
  lastName: string;
  email: string;
  grnPermissions: Immutable.List<string>;
  permissions: Immutable.List<string>;
  timezone: string | null | undefined;
  preferences: PreferencesMap;
  roles: Immutable.Set<string>;
  readOnly: boolean;
  external: boolean;
  sessionTimeoutMs: number;
  startpage?: StartPage;
  sessionActive: boolean;
  clientAddress: string;
  lastActivity: string | null | undefined;
  accountStatus: AccountStatus;
  serviceAccount: boolean;
  authServiceEnabled: boolean;
};

export default class User {
  _value: InternalState;

  constructor(
    id: InternalState['id'],
    username: InternalState['username'],
    fullName: InternalState['fullName'],
    firstName: InternalState['firstName'],
    lastName: InternalState['lastName'],
    email: InternalState['email'],
    permissions: InternalState['permissions'],
    timezone: InternalState['timezone'],
    preferences: InternalState['preferences'],
    roles: InternalState['roles'],
    readOnly: InternalState['readOnly'],
    external: InternalState['external'],
    sessionTimeoutMs: InternalState['sessionTimeoutMs'],
    startpage: InternalState['startpage'],
    sessionActive: InternalState['sessionActive'],
    clientAddress: InternalState['clientAddress'],
    lastActivity: InternalState['lastActivity'],
    accountStatus: InternalState['accountStatus'],
    serviceAccount: InternalState['serviceAccount'],
    grnPermissions: InternalState['grnPermissions'],
    authServiceEnabled: InternalState['authServiceEnabled'],
  ) {
    this._value = {
      id,
      username,
      fullName,
      firstName,
      lastName,
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
      accountStatus,
      grnPermissions,
      serviceAccount,
      authServiceEnabled,
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

  get firstName() {
    return this._value.firstName;
  }

  get lastName() {
    return this._value.lastName;
  }

  get email() {
    return this._value.email;
  }

  get grnPermissions() {
    return this._value.grnPermissions;
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

  get accountStatus() {
    return this._value.accountStatus;
  }

  get serviceAccount() {
    return this._value.serviceAccount;
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

    const _estimateUnit = (value): [number, string] => {
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

  get authServiceEnabled() {
    return this._value.authServiceEnabled;
  }

  toBuilder() {
    const {
      id,
      username,
      fullName,
      firstName,
      lastName,
      email,
      permissions,
      grnPermissions,
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
      accountStatus,
      serviceAccount,
      authServiceEnabled,
    } = this._value;

    // eslint-disable-next-line @typescript-eslint/no-use-before-define
    return new Builder(
      Immutable.Map({
        id,
        username,
        fullName,
        firstName,
        lastName,
        email,
        permissions,
        timezone,
        grnPermissions,
        preferences,
        roles,
        readOnly,
        external,
        sessionTimeoutMs,
        startpage,
        sessionActive,
        clientAddress,
        lastActivity,
        accountStatus,
        serviceAccount,
        authServiceEnabled,
      }),
    );
  }

  static create(
    id: InternalState['id'],
    username: InternalState['username'],
    fullName: InternalState['fullName'],
    firstName: InternalState['firstName'],
    lastName: InternalState['lastName'],
    email: InternalState['email'],
    permissions: InternalState['permissions'],
    timezone: InternalState['timezone'],
    preferences: InternalState['preferences'],
    roles: InternalState['roles'],
    readOnly: InternalState['readOnly'],
    external: InternalState['external'],
    sessionTimeoutMs: InternalState['sessionTimeoutMs'],
    startpage: InternalState['startpage'],
    sessionActive: InternalState['sessionActive'],
    clientAddress: InternalState['clientAddress'],
    lastActivity: InternalState['lastActivity'],
    accountStatus: InternalState['accountStatus'],
    serviceAccount: InternalState['serviceAccount'],
    grnPermissions: InternalState['grnPermissions'],
    authServiceEnabled: InternalState['authServiceEnabled'],
  ) {
    return new User(
      id,
      username,
      fullName,
      firstName,
      lastName,
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
      accountStatus,
      serviceAccount,
      grnPermissions,
      authServiceEnabled,
    );
  }

  static empty() {
    // @ts-ignore
    return User.create(
      '',
      '',
      '',
      '',
      Immutable.List(),
      '',
      {},
      Immutable.Set(),
      false,
      false,
      -1,
      undefined,
      false,
      '',
      '',
      'enabled',
    );
  }

  toJSON(): UserJSON {
    const {
      id,
      username,
      fullName,
      firstName,
      lastName,
      email,
      grnPermissions,
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
      accountStatus,
      serviceAccount,
      authServiceEnabled,
    } = this._value;

    return {
      id,
      username,
      full_name: fullName,
      first_name: firstName,
      last_name: lastName,
      email,
      grn_permissions: grnPermissions ? grnPermissions.toJS() : [],
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
      account_status: accountStatus,
      service_account: serviceAccount,
      auth_service_enabled: authServiceEnabled,
    };
  }

  static fromJSON(value: UserJSON) {
    const {
      id,
      username,
      full_name,
      first_name,
      last_name,
      email,
      grn_permissions,
      permissions,
      timezone,
      preferences,
      roles,
      read_only,
      external,
      session_timeout_ms,
      startpage,
      session_active,
      client_address,
      last_activity,
      account_status,
      service_account,
      auth_service_enabled,
    } = value;

    return User.create(
      id,
      username,
      full_name,
      first_name,
      last_name,
      email,
      Immutable.List(permissions),
      timezone,
      preferences,
      Immutable.Set(roles),
      read_only,
      external,
      session_timeout_ms,
      startpage,
      session_active,
      client_address,
      last_activity,
      account_status,
      service_account,
      Immutable.List(grn_permissions),
      auth_service_enabled,
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

  id(value: InternalState['id']) {
    return new Builder(this.value.set('id', value));
  }

  username(value: InternalState['username']) {
    return new Builder(this.value.set('username', value));
  }

  fullName(value: InternalState['fullName']) {
    return new Builder(this.value.set('fullName', value));
  }

  firstName(value: InternalState['firstName']) {
    return new Builder(this.value.set('firstName', value));
  }

  lastName(value: InternalState['lastName']) {
    return new Builder(this.value.set('lastName', value));
  }

  email(value: InternalState['email']) {
    return new Builder(this.value.set('email', value));
  }

  grnPermissions(value: InternalState['grnPermissions']) {
    return new Builder(this.value.set('grnPermissions', value));
  }

  permissions(value: InternalState['permissions']) {
    return new Builder(this.value.set('permissions', value));
  }

  timezone(value: InternalState['timezone']) {
    return new Builder(this.value.set('timezone', value));
  }

  preferences(value: InternalState['preferences']) {
    return new Builder(this.value.set('preferences', value));
  }

  roles(value: InternalState['roles']) {
    return new Builder(this.value.set('roles', value));
  }

  readOnly(value: InternalState['readOnly']) {
    return new Builder(this.value.set('readOnly', value));
  }

  external(value: InternalState['external']) {
    return new Builder(this.value.set('external', value));
  }

  sessionTimeoutMs(value: InternalState['sessionTimeoutMs']) {
    return new Builder(this.value.set('sessionTimeoutMs', value));
  }

  startpage(value: InternalState['startpage']) {
    return new Builder(this.value.set('startpage', value));
  }

  sessionActive(value: InternalState['sessionActive']) {
    return new Builder(this.value.set('sessionActive', value));
  }

  clientAddress(value: InternalState['clientAddress']) {
    return new Builder(this.value.set('clientAddress', value));
  }

  lastActivity(value: InternalState['lastActivity']) {
    return new Builder(this.value.set('lastActivity', value));
  }

  accountStatus(value: InternalState['accountStatus']) {
    return new Builder(this.value.set('accountStatus', value));
  }

  serviceAccount(value: InternalState['serviceAccount']) {
    return new Builder(this.value.set('serviceAccount', value));
  }

  authServiceEnabled(value: InternalState['authServiceEnabled']) {
    return new Builder(this.value.set('authServiceEnabled', value));
  }

  build() {
    const {
      id,
      username,
      fullName,
      firstName,
      lastName,
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
      accountStatus,
      serviceAccount,
      grnPermissions,
      authServiceEnabled,
    } = this.value.toObject();

    return new User(
      id,
      username,
      fullName,
      firstName,
      lastName,
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
      accountStatus,
      serviceAccount,
      grnPermissions,
      authServiceEnabled,
    );
  }
}

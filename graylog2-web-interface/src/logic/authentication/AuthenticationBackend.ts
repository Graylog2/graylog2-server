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
import { DirectoryServiceAuthenticationService } from 'components/authentication/types';

import { getAuthServicePlugin } from 'util/AuthenticationService';
import { DirectoryServiceBackendConfig } from 'logic/authentication/directoryServices/types';
import { OktaBackendConfig } from 'logic/authentication/okta/types';

type InternalState = {
  id: string,
  title: string,
  description: string,
  defaultRoles: Immutable.List<string>,
  config: DirectoryServiceBackendConfig | OktaBackendConfig,
};

type TypedConfig = {
  type: string,
};

export type AuthenticationBackendJSON = {
  id: string,
  title: string,
  description: string,
  default_roles: Array<string>,
  config: DirectoryServiceBackendConfig | OktaBackendConfig,
};

const configFromJson = (config: $PropertyType<AuthenticationBackendJSON, 'config'>) => {
  const authService = getAuthServicePlugin((config as TypedConfig).type, true);

  if (authService && typeof authService.configFromJson === 'function') {
    return (authService as DirectoryServiceAuthenticationService).configFromJson(config);
  }

  return config;
};

const configToJson = (config: $PropertyType<AuthenticationBackendJSON, 'config'>) => {
  const authService = getAuthServicePlugin((config as TypedConfig).type, true);

  if (authService && typeof authService.configToJson === 'function') {
    return authService.configToJson(config);
  }

  return config;
};

export default class AuthenticationBackend {
  _value: InternalState;

  constructor(
    id: $PropertyType<InternalState, 'id'>,
    title: $PropertyType<InternalState, 'title'>,
    description: $PropertyType<InternalState, 'description'>,
    defaultRoles: $PropertyType<InternalState, 'defaultRoles'>,
    config: $PropertyType<InternalState, 'config'>,
  ) {
    this._value = {
      id,
      title,
      description,
      defaultRoles,
      config,
    };
  }

  get id(): $PropertyType<InternalState, 'id'> {
    return this._value.id;
  }

  get title(): $PropertyType<InternalState, 'title'> {
    return this._value.title;
  }

  get description(): $PropertyType<InternalState, 'description'> {
    return this._value.description;
  }

  get defaultRoles(): $PropertyType<InternalState, 'defaultRoles'> {
    return this._value.defaultRoles;
  }

  get config(): $PropertyType<InternalState, 'config'> {
    return this._value.config;
  }

  toBuilder(): Builder {
    const {
      id,
      title,
      description,
      defaultRoles,
      config,
    } = this._value;

    // eslint-disable-next-line @typescript-eslint/no-use-before-define
    return new Builder(Immutable.Map({
      id,
      title,
      description,
      defaultRoles,
      config,
    }));
  }

  toJSON() {
    const {
      id,
      title,
      description,
      defaultRoles = Immutable.List(),
      config,
    } = this._value;

    const formattedConfig = configToJson(config);

    return {
      id,
      title,
      description,
      default_roles: defaultRoles.toJS(),
      config: formattedConfig,
    };
  }

  static fromJSON(value: AuthenticationBackendJSON) {
    const {
      id,
      title,
      description,
      default_roles: defaultRoles,
      config,
    } = value;

    const formattedConfig = configFromJson(config);

    return new AuthenticationBackend(
      id,
      title,
      description,
      Immutable.List(defaultRoles),
      formattedConfig,
    );
  }

  static builder(): Builder {
    // eslint-disable-next-line @typescript-eslint/no-use-before-define
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

  title(value: $PropertyType<InternalState, 'title'>): Builder {
    return new Builder(this.value.set('title', value));
  }

  description(value: $PropertyType<InternalState, 'description'>): Builder {
    return new Builder(this.value.set('description', value));
  }

  defaultRoles(value: $PropertyType<InternalState, 'defaultRoles'>): Builder {
    return new Builder(this.value.set('defaultRoles', value));
  }

  config(value: $PropertyType<InternalState, 'config'>): Builder {
    return new Builder(this.value.set('config', value));
  }

  build(): AuthenticationBackend {
    const {
      id,
      title,
      description,
      defaultRoles,
      config,
    } = this.value.toObject();

    return new AuthenticationBackend(
      id,
      title,
      description,
      defaultRoles,
      config,
    );
  }
}
